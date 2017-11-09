from heapq import heappush, heappop, heapify
from collections import defaultdict
import os

MAX_BITS = 8

def huffman(symb2freq):
    """Huffman encode the given dict mapping symbols to weights"""
    heap = [[wt, [sym, ""]] for sym, wt in symb2freq.items()]
    heapify(heap)
    while len(heap) > 1:
        lo = heappop(heap)
        hi = heappop(heap)
        for pair in lo[1:]:
            pair[1] = '0' + pair[1]
        for pair in hi[1:]:
            pair[1] = '1' + pair[1]
        heappush(heap, [lo[0] + hi[0]] + lo[1:] + hi[1:])
    return sorted(heappop(heap)[1:], key=lambda p: (len(p[-1]), p))

def encoder(file):
    if os.path.exists(file):
        fp = open(file, 'r')
        long_str = fp.read()
        fp.close()
        print ('\nText: ' + long_str)

        symb2freq = defaultdict(int)
        for ch in long_str:
            symb2freq[ch] += 1
        huff = huffman(symb2freq)
        codemap = {}
        for p in huff:
            char = p[0]
            codemap[p[0]] = p[1]
        counter = 1
        codestr = ''
        encodedtext = ''
        for ch in long_str:
            code = codemap[ch]
            codestr = codestr + code
        binarytext = ''
        counter2 = 0
        while (MAX_BITS - (len(codestr) % MAX_BITS) > counter2):
                counter2 = counter2 + 1;
                binarytext = '0' + binarytext
        counter = counter2
        for ch in codestr:
            counter = counter + 1
            binarytext = binarytext + ch
            if counter%MAX_BITS == 0:
                num = int(binarytext,2)
                encodedtext = encodedtext + chr(num)
                binarytext = ''
                flag = 1
        encodedtext_file = open("encodedtext.txt","w",encoding='utf-8')
        encodedtext_file.write("%s" % encodedtext)
        encodedtext_file.close()
        
    else:
        print ('file not found')


    return symb2freq,len(codestr)
    
def decoder(file,symb2freq,length):
    huff = huffman(symb2freq)
    if os.path.exists(file):
        fp = open(file, 'r',encoding='utf-8')
        long_str = fp.read()
        fp.close()
        print ('\nEncoded Text: '+ long_str  + '\n')
        binary_text =''
        get_bin = lambda x: format(x, 'b')
        delete = MAX_BITS - length % MAX_BITS
        flag = 0
        for ch in long_str:
            num = ord(ch)
            binary = str(get_bin(num))
            if len(binary) != 8:
                binary = ((8 - len(binary)) * '0') + binary
                if flag == 0:
                    binary = binary[delete:]
                    flag = 1
                binary_text = binary_text + binary
            else:
                binary_text = binary_text + binary
        codemap = {}
        for p in huff:
            char = p[1]
            codemap[p[1]] = p[0]
        name = ''
        decodedtext = ''
        for ch in binary_text:
            name = name + ch
            if name in codemap:
                decodedtext = decodedtext + codemap[name]
                name = ''
        print('Decoded Text: ' + decodedtext)
    else:
        print ('file not found')
if __name__=='__main__':

    
    original_file = 'text.txt'
    symb2freq,length = encoder(original_file)
    encoded_file = 'encodedtext.txt'
    decoder(encoded_file,symb2freq,length)
