# Text compression using Huffman coding 
In this repository there is an Android application called Hcoder and server in order to simulate a text compression using Huffman coding. Briefly, application takes a text file and send to another android device through server after compressing. The file can send from server to Android device, Android device to another Android device or from device to server in order to be encoded/decoded. For the communication between devices and the server, socket communication is used. Devices are sending bytes each other over sockets.

Geetting Started
----------------

Follow instuction below in order to setup applicaton.

* Download or clone repository.
* Start server, using `server.py` file. In order to start server use: `python3 server.py`
* Build and Run Android Client using one of Android emulator.
* Start web client using file `site.py` in folder \_server: `python3 site.py`

Usage
-----

After setting up and running all neccesary component, user can send a text file either using file picker in Android App or using file picker in web client.

When reciever gets file it will be saved on device.

Built With
----------

* Android Studio / Java
* Python / Flask + [Socket](https://docs.python.org/3/library/socket.html)
* [Huffman Coding](https://en.wikipedia.org/wiki/Huffman_coding)
