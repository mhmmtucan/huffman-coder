import socket, threading, json


class ThreadedServer(object):
    def __init__(self, host, port, usersPort, usersList, clients):
        print("Server Starting...")
        print("Host: ", host)
        print("Port: ", port)
        self.host = host
        self.port = port
        self.usersPort = usersPort
        self.usersList = usersList
        self.clients = clients
        self.sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        #self.sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
        self.sock.bind((self.host, self.port))

    def listen(self):
        self.sock.listen(5)
        try:
            while True:
                client, address = self.sock.accept()
                #client.settimeout(60)
                threading.Thread(target = self.listenToClient,args = (client,address)).start()
        except KeyboardInterrupt:
            print("\nServer is closing...")
            self.sock.close()

    def listenToClient(self, client, address):
        size = 4096
        while True:
            try:
                #recieve data
                data = client.recv(size).decode('utf-8')
                if data:
                    data = data[:-1]
                    print ('Got connection from', address)
                    #take port and host, for further use save it
                    host,port = address

                    #if data is string it is username, if json it is txt file
                    try:
                        json_data = json.loads(data)
                        #it is encoded txt use it
                        #send file
                        reciever = json_data["reciever"]
                        clients[reciever].sendall(json.dumps(json_data).encode() + '\n'.encode())
                    except ValueError:
                        username = data
                        print(username, "is connected.")
                        # if new comer, add to userlist
                        if username not in self.usersList:
                            self.usersList.append(username)
                        
                        self.usersPort[username] = port
                        self.clients[username] = client
                        # send to all user
                        usersListJSON = dict()
                        usersListJSON["users"] = self.usersList
                        print("Current-users",usersListJSON["users"])
                        for c in clients.values():
                            c.send(json.dumps(usersListJSON).encode() + '\n'.encode())

                else:
                    
                    raise error('Client disconnected')
            except:
                # disconnect
                usersListJSON = dict()
                if username in self.usersList:
                    self.usersList.remove(username)
                usersListJSON["users"] = self.usersList
                if username in clients:
                    del clients[username]
                for c in clients.values():
                    c.send(json.dumps(usersListJSON).encode() + '\n'.encode())
                print("Client connection closing: ", username)
                print("Remaining-users",usersListJSON["users"])
                client.close()
                return False
                

if __name__ == "__main__":
    usersPort = dict()
    usersList = list()
    clients = dict()
    ThreadedServer('127.0.0.1',12345, usersPort, usersList, clients).listen()
    #ThreadedServer('0.0.0.0',12345, usersPort, usersList, clients).listen()
