from heapq import *

try:
    import Queue as Q
except:
    import queue as Q

class Data:
    def __init__(self, d, b):
        self.encoded_data = d
        self.num_of_bits = b

class HuffmanTree:
    def __init__(self, freq):
        self.freq = freq

    def __lt__(self, other):
        return self.freq < other.freq
    def __le__(self, other):
        return self.freq <= other.freq
    def __eq__(self, other):
        return self.freq == other.freq
    def __ne__(self, other):
        return self.freq != other.freq
    def __gt__(self, other):
        return self.freq > other.freq
    def __ge__(self, other):
        return self.freq >= other.freq
    
class HuffmanLeaf(HuffmanTree):
    def __init__(self, freq = 1, val = '\0'):
        HuffmanTree.__init__(self, freq)
        self.val = val

class HuffmanNode(HuffmanTree):
    def __init__(self, l = None, r = None):
        if l is not None and r is not None:
            HuffmanTree.__init__(self, l.freq + r.freq)
        else:
            HuffmanTree.__init__(self, 1)
            
        self.left = l
        self.right = r

class HuffmanEncoder:
    codes = {}

    def BuildTree(self, freqs):
        q = Q.PriorityQueue()

        for i in range(0, len(freqs)):
            if(freqs[i] > 0):
                q.put(HuffmanLeaf(freqs[i], chr(i)))

        while(q.qsize() > 1):
            a = q.get()
            b = q.get()

            q.put(HuffmanNode(a, b))

        return q.get()

    def StoreCodes(self, tree, prefix):
        if isinstance(tree, HuffmanLeaf):
            self.codes[tree.val] = prefix
        elif isinstance(tree, HuffmanNode):
            self.StoreCodes(tree.left, prefix + '0')
            self.StoreCodes(tree.right, prefix + '1')

    def Encode(self, tree, string):
        ret = ''

        for c in string:
            ret += self.codes[c]

        chars = ''
        offset = 0

        i = len(ret)
        while i > 0:
            s = ret[0 if i - 8 < 0 else i - 8 : i]
            #print(s)
            index = i // 8
            num = 0

            for c in s:
                num <<= 1

                if c == '1':
                    num |= 1

            chars = chr(num) + chars
                    
            i = i - 8

        return Data(chars, len(ret))
        
    def Run(self, to_encode):
        freqs = [0 for i in range(0, 256)]
        for c in to_encode:
            freqs[ord(c)] += 1
        
        tree = self.BuildTree(freqs)
        self.StoreCodes(tree, '')

        ret = self.Encode(tree, to_encode)
        ret.map = self.codes
        return ret

class HuffmanDecoder:
    codes = {}

    def BuildTree(self, m):
        self.codes = m

        node = HuffmanNode()

        for key in self.codes:
            value = self.codes[key]

            current_node = node

            for i in range(0, len(value)):
                c = value[i]
                if i == len(value) - 1:
                    if c == '0':
                        current_node.left = HuffmanLeaf(self, val = key)
                    elif c == '1':
                        current_node.right = HuffmanLeaf(self, val = key)
                else:
                    if c == '0':
                        if current_node.left is None:
                            current_node.left = HuffmanNode()
                        current_node = current_node.left
                    elif c == '1':
                        if current_node.right is None:
                            current_node.right = HuffmanNode()
                        current_node = current_node.right
        return node

    def HuffmanDecode(self, tree, string, num):
        binary = ''
        ret = ''
        current = tree

        for i in range(0, len(string)):
            part = ''
            c = string[i]
            n = ord(c)

            if i == 0:
                x = num % 8

                for j in range(0, x):
                    part = str(n & 1) + part
                    n >>= 1
            else:
                for j in range(0, 8):
                    part = str(n & 1) + part
                    n >>= 1
                    
            binary = binary + part

        for c in binary:
            if isinstance(current, HuffmanNode):
                if c == '0':
                    current = current.left
                elif c == '1':
                    current = current.right

                if isinstance(current, HuffmanLeaf):
                    ret += current.val
                    current = tree
        return ret

    def Run(self, data):
        tree = self.BuildTree(data.map)
        decoded_string = self.HuffmanDecode(tree, data.encoded_data, data.num_of_bits)
        return decoded_string
