
1) Find gerrit.cer file in project directory.

1.1) If you don't have ready ".cer" file: Go to URL in your firefox browser, click on HTTPS certificate chain (next to URL address). 
Click "more info" > "security" > "show certificate" > "details" > "export..". 
Pickup the name and choose file type "gerrit.cer". Now you have file with keystore and you have to add it to your JVM

2) Determine location of cacerts files, eg.  C:\Program Files (x86)\Java\jre1.6.0_22\lib\security\cacerts.

3) Next import the example.cer file into cacerts in command line:
keytool -import -alias example -keystore  <path\to\java\jre_version>\lib\security\cacerts -file gerrit.cer

You will be asked for password which default is "changeit"