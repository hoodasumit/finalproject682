import json
import os, io
import re
import webbrowser

from google.cloud import vision
from google.cloud import storage
from google.protobuf import json_format

# https://console.cloud.google.com/storage/browser/ocr-umass-vision-sb login to this for getting the file details.
bucket_root =  "gs://ocr-umass-vision-sb" #if you want it to be dynamic use this:  input("Please enter the root of bucket [EXAMPLE: gs://ocr-pdf-vision ]: ")
bucket_uri = input("Please enter the gsutil url of the file: ")

#uncomment to use custom identifers
#file_identifier = input("Please enter an  Identification name for the output folder/HTML file: ")

x = bucket_uri.split('/')
file_identifier = x[-1].rstrip('.pdf')



os.environ['GOOGLE_APPLICATION_CREDENTIALS'] = r'service.json'
client = vision.ImageAnnotatorClient()

batch_size = 2
mime_type = 'application/pdf'
feature = vision.Feature(
    type=vision.Feature.Type.DOCUMENT_TEXT_DETECTION)

gcs_source_uri = bucket_uri
gcs_source = vision.GcsSource(uri=gcs_source_uri)
input_config = vision.InputConfig(gcs_source=gcs_source, mime_type=mime_type)

gcs_destination_uri = '{}/{}/'.format(bucket_root,file_identifier)
gcs_destination = vision.GcsDestination(uri=gcs_destination_uri)
output_config = vision.OutputConfig(gcs_destination=gcs_destination, batch_size=batch_size)

async_request = vision.AsyncAnnotateFileRequest(
    features=[feature], input_config=input_config, output_config=output_config)

operation = client.async_batch_annotate_files(requests=[async_request])
operation.result(timeout=180)

storage_client = storage.Client()
match = re.match(r'gs://([^/]+)/(.+)', gcs_destination_uri)
bucket_name = match.group(1)
prefix = match.group(2)
bucket = storage_client.get_bucket(bucket_name)


blob_list = list(bucket.list_blobs(prefix=prefix))
print('Output files:')
for blob in blob_list:
    print(blob.name)

output = blob_list[0]

text_split = ""
for i in range(len(blob_list)):
    output = blob_list[i]
    json_string = output.download_as_string()

    json_pdfout = json.loads(json_string)


    for m in range(len(json_pdfout['responses'])):
        first_page_response = json_pdfout['responses'][m]
        annotation = first_page_response['fullTextAnnotation']
        text_split = text_split + "\n" + annotation['text']

ocr_split = text_split.splitlines()
with open("{}.html".format(file_identifier), "w", encoding='utf-8') as html_file:
    html_file.write("<html> <head> </head> <body>")
    for i in ocr_split:
         html_file.write("<br>")
         html_file.write(f'{i} \n')
         html_file.write("\n <br>")


    html_file.write("</body></html>")

webbrowser.open("{}.html".format(file_identifier))