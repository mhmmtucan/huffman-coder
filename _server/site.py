#!/usr/bin/python           # This is server.py file
import socket, json, threading, time, queue
from flask import Flask, render_template, request
from huffman import *
app = Flask(__name__)
app.config['SECRET_KEY'] = 'mysecret'

userlist = list()
decodedText = ""
sender = ""

@app.route('/')
def home():
    global userlist
    if not q.empty():
        userlist = q.get()
    
    if "web" in userlist:
        userlist.remove("web")
    return render_template('home.html', my_list=userlist, decodedText=decodedText, sender=sender)

@app.route('/send', methods = ['POST'])
def getFile():
    file = request.files['file']
    reciever = request.form['optradio']
    # encode file, take reciever username
    # make json
    encoder = HuffmanEncoder()
    #encodedText,freqMap,byteLength = encoder(file.read().decode('utf-8'))
    data = encoder.Run(file.read().decode('utf-8'))
    encodedText,freqMap,byteLength = (data.encoded_data, data.map, data.num_of_bits)
    data = dict()
    data["sender"] = "web"
    data["reciever"] = reciever
    data["map"] = json.dumps(freqMap)
    data["len"] = byteLength
    data["text"] = encodedText
    s.sendall(json.dumps(data).encode('utf-8') + '\n'.encode())
    return render_template('home.html', prompt="File sended")

class ThreadedClient(object):
    def __init__(self, s, q):
        self.q = q
        self.s = s
    def run(self):
        threading.Thread(target = self.listenSocket,args = (self.s,self.q)).start()

    def listenSocket(self,s,q):
        while True:
            data = s.recv(4096).decode('utf-8')
            if data:
                data = data[:-1]
                json_data = json.loads(data)
                if "users" in json_data:
                    # someone connectod to server, update list at home
                    q.put(json.loads(json.dumps(json_data["users"])))
                else:
                    # decode json
                    # python huffman
                    jdata = json.loads(data)
                    global decodedText,sender
                    sender = jdata["sender"]
                    decoder = HuffmanDecoder()
                    data = Data(jdata['text'], jdata['len'])
                    data.map = json.loads(jdata['map'])
                    #data.encoded_data = jdata['text']
                    #data.num_of_bits = jdata['len']
                    #decodedText = decoder(jdata['text'],json.loads(jdata['map']),jdata['len'])
                    decodedText = decoder.Run(data)

                    encodedtext_file = open("encodedtext.txt","w",encoding='utf-8')
                    encodedtext_file.write("%s" % decodedText)
                    encodedtext_file.close()

                    #encoded_file = open('web-' + json_data['sender'] + '-to-' + json_data['reciever'] + '-encoded.txt', 'w', 'utf-8')
                    #encoded_file.write(json_data['sender'] + '\n')
                    #encoded_file.write(json_data['reciever'] + '\n')
                    #encoded_file.write(json_data['map'] + '\n')
                    #encoded_file.write(str(json_data['len']) + '\n')
                    #encoded_file.write(json_data['text'] + '\n')
                    #encoded_file.close()

                    #decoded_file = open('web-' + json_data['sender'] + '-to-' + json_data['reciever'] + '-decoded.txt', 'w', 'utf-8')
                    #decoded_file.write(decoded_text + '\n')
                    #decoded_file.close()

                    print(decodedText)
                

if __name__ == '__main__':
    s = socket.socket()         # Create a socket object
    #host = "127.0.0.1"
    host = "192.168.2.106"          # Get local machine name
    port = 12345                # Reserve a port for your service.
    s.connect((host, port))
    s.sendall("web".encode() + '\n'.encode())
    q = queue.Queue()
    userlist = list()

    ThreadedClient(s,q).run()

    app.run(debug=True, host="0.0.0.0", port=8080)
    
