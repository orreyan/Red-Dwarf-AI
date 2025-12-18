import os
import io

import streamlit as st
import google.generativeai as genai
import google.ai.generativelanguage as glm
import requests

from dotenv import load_dotenv
from PyPDF2 import PdfReader
from streamlit_option_menu import option_menu
from PIL import Image

class RedDwarfAI:
    def __init__(self):
        load_dotenv()

        st.set_page_config(
            page_title="Chat with Gemini-Pro!",
            page_icon=":brain:",  
            layout="wide",  
        )

        GOOGLE_API_KEY = os.getenv("GOOGLE_API_KEY")
        genai.configure(api_key=GOOGLE_API_KEY)
        self.model = genai.GenerativeModel('gemini-pro')

    def translate_role_for_streamlit(self, user_role):
        if user_role == "model":
            return "assistant"
        else:
            return user_role

    def start(self):

        st.markdown("""
        <h1 style='
            font-family: Arial; 
            font-size: 50px; 
            color: #FF4B4B; 
            font-weight: bold; 
            text-align: center; 
            margin-top: -78px
        '>✦ Red Dwarf AI</h1>
        """, unsafe_allow_html=True)

        selected = option_menu(
            menu_title=None,  
            options=["ChatBot", "PDF Reader", "Image Nerd", "Cropto Indicator"],
            icons=["star", "book", "image", "info"],  
            menu_icon="cast", 
            orientation="horizontal",)

        
        if selected == "ChatBot":
            self.chat_bot()

        elif selected == "PDF Reader":
            self.pdf_reader()

        elif selected == "Image Nerd":
            self.image_nerd()

        elif selected == "Cropto Indicator":
            self.cropto_indicator()
    

    def chat_bot(self):

        if "chatBot_session" not in st.session_state:
            st.session_state.chatBot_session = self.model.start_chat(history=[])

        for message in st.session_state.chatBot_session.history:
            with st.chat_message(self.translate_role_for_streamlit(message.role)):
                st.markdown(message.parts[0].text)

        user_prompt = st.chat_input("Ask Red Dwarf...")
        if user_prompt:

            st.chat_message("user").markdown(user_prompt)

            gemini_response = st.session_state.chatBot_session.send_message(user_prompt)

            with st.chat_message("assistant"):
                st.markdown(gemini_response.text)
        
        if st.session_state.chatBot_session.history:
            if st.button("Clear Chat", help="Clear the chat history"):
                st.session_state.chatBot_session.history.clear()

    def pdf_reader(self):

        if "chat_session_pdf" not in st.session_state:
            st.session_state.chat_session_pdf = self.model.start_chat(history=[])

 
        def read_pdf(uploaded_file):
            try:
                reader = PdfReader(uploaded_file)
                num_pages = len(reader.pages)
                text = ''
                for page_num in range(num_pages):
                    page = reader.pages[page_num]
                    text += page.extract_text()
                return text
            except Exception as e:
                st.error(f"Error occurred while reading PDF: {e}")
                return None

 
        pdf_option = st.radio("Select PDF upload option:", ("Upload from local device", "Upload from online link"))
        if pdf_option == "Upload from local device":
            pdf = st.file_uploader("Upload your PDF", type='pdf')
            if pdf:
                pdf_text = read_pdf(pdf)
                if pdf_text:
                    if st.button("Read the PDF", help="Summerizes the PDF"):
                        gemini_response = st.session_state.chat_session_pdf.send_message(pdf_text)

        elif pdf_option == "Upload from online link":
            pdf_url = st.text_input("Enter the URL of the PDF:")
            if st.button("Read from URL", help="Summerizes the PDF"):
                try:
                    response = requests.get(pdf_url)
                    response.raise_for_status() 
                    pdf_text = read_pdf(io.BytesIO(response.content))
                    if pdf_text:
                        gemini_response = st.session_state.chat_session_pdf.send_message(pdf_text)
                except Exception as e:
                    st.error("Unable to access the PDF, try a diffrent link or reupload the link.")


        for message in st.session_state.chat_session_pdf.history:
            with st.chat_message(self.translate_role_for_streamlit(message.role)):
                st.markdown(message.parts[0].text)

        user_prompt = st.chat_input("Ask Red Dwarf...")
        if user_prompt:
            st.chat_message("user").markdown(user_prompt)
            gemini_response = st.session_state.chat_session_pdf.send_message(user_prompt)
            with st.chat_message("assistant"):
                st.markdown(gemini_response.text)

        if st.session_state.chat_session_pdf.history:
            if st.button("Clear Chat", help="Click twice to clear chat history"):
                st.session_state.chat_session_pdf.history.clear()

    def image_nerd(self):

        uploaded_file = st.file_uploader("Choose an Image", accept_multiple_files=False, type=["png", "jpg", "jpeg", "img", "webp"])     

        if uploaded_file is not None:
            image = Image.open(uploaded_file)
            width, height = image.size
            new_width = 750  
            new_height = int((new_width / width) * height)
            image = image.resize((new_width, new_height))
            
 
            col1, col2, col3 = st.columns([1, 2, 1]) 
            
            with col1:
                pass  
            
            with col2:
                st.image(image)
                
                hide_img_fs = '''
                <style>
                button[title="View fullscreen"]{
                    visibility: hidden;}
                </style>
                '''
                st.markdown(hide_img_fs, unsafe_allow_html=True)
            
            with col3:
                pass  
            
            st.markdown("""<style>img {border-radius: 10px;}</style>""", unsafe_allow_html=True)

        image_prompt = st.text_input("Interact with the Image", placeholder="Prompt", label_visibility="visible")

        if uploaded_file is not None:
            if st.button("Process the Image"):
                if uploaded_file is not None and image_prompt != "":
                    model = genai.GenerativeModel()
                    image = Image.open(uploaded_file)
                    

                    response = model.generate_content(
                        glm.Content(
                            parts=[
                                glm.Part(text=image_prompt),
                                glm.Part(
                                    inline_data=glm.Blob(
                                        mime_type="image/jpeg",
                                        data=self.image_to_byte_array(image)
                                    )
                                )
                            ]
                        )
                    )
                    response.resolve()
                    with st.chat_message("assistant"):
                        st.markdown(response.text)
                else:
                    st.write("")
                    st.header(":red[Please provide both an image and a prompt]")

        

    def image_to_byte_array(self, image: Image) -> bytes:
        img_byte_array = io.BytesIO()
        image.save(img_byte_array, format=image.format)
        return img_byte_array.getvalue()

    def cropto_indicator(self):
        st.header("""Still in progress of development. It will be included in the next update.
                This Project will only include meme coins on solona blockchain, it will validate the projects and allow users to identify strong projects that have a 
    higher chance of performing good.
                  
    Features to Expect:
        Swapping of crypto using BonkBot (Allows for faster transactions)
        Validation of the crypto project (Whether it's a rug pull or not)
        Accurate charts and all necessary data for human check
        Calls on newly launched projects.     
                  """)

if __name__ == "__main__":
    reddwarf_ai = RedDwarfAI()
    reddwarf_ai.start()

