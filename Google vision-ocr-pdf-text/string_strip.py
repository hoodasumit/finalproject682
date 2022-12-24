str = "gs://ocr-umass-vision-sb/source/Assorted-20221216T193556Z-001/Assorted/Contract-Agreement-and-Memorandum-of-Understanding-Review-and-Approval.pdf"


x = str.split('/')
print(x[-1].rstrip('.pdf'))
