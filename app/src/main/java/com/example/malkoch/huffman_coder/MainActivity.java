package com.example.malkoch.huffman_coder;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

class Client extends AsyncTask<String, Void, String> {
    Socket socket;

    public Client(String hostname, int port_number) {
        try {
            socket = new Socket(hostname, port_number);
        }
        catch (Exception e) {
            Log.d("error", e.toString());
        }
    }

    @Override
    protected String doInBackground(String... args) {
        String data = args[0];

        String ret = "";

        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println(data);
            out.flush();

            while(true) {
                String response = in.readLine();
                //Log.d("serverresponse", "response : " + response);

                JSONObject json_object = new JSONObject(response);

                if(json_object.has("users")) {
                    //getting users
                    Log.d("serverresponse", "getting the userlist");
                }
                else {
                    //getting data
                    String sender = json_object.getString("sender");
                    String reciever = json_object.getString("reciever");
                    String map = json_object.getString("map");
                    int number = json_object.getInt("len");
                    String text = json_object.getString("text");

                    JSONObject json_map = new JSONObject(map);

                    Log.d("serverresponse", "sender: " + sender);
                    Log.d("serverresponse", "reciever: " + reciever);
                    Log.d("serverresponse", "map: " + map);
                    Log.d("serverresponse", "number: " + number);
                    Log.d("serverresponse", "text: " + text);

                    //Log.d("serverresponse", "getting the data");
                }
            }
        }
        catch (Exception e) {
            Log.d("error", e.toString());
        }

        return ret;
    }

    @Override
    protected void onPostExecute(String result) {

    }
}

class FileUtils {
    public static String GetPath(Context context, Uri uri) throws URISyntaxException {
        if("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {"_data"};
            Cursor cursor = null;

            try {
                cursor = context.getContentResolver().query(uri, projection, null, null, null);
                int column_index = cursor.getColumnIndexOrThrow("_data");
                if(cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
            }
            catch (Exception e) {
                Log.d("fileinformation", "getting path: " + e.toString());
            }
        }
        else if("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }
}

class Data {
    String encoded_data;
    int num_of_bits;
    int[] freqs;

    public Data(String d, int b) {
        encoded_data = d;
        num_of_bits = b;
    }
}

abstract class HuffmanTree implements Comparable<HuffmanTree> {
    public final int frequency; // the frequency of this tree
    public HuffmanTree(int freq) { frequency = freq; }

    // compares on the frequency
    public int compareTo(HuffmanTree tree) {
        return frequency - tree.frequency;
    }
}

class HuffmanLeaf extends HuffmanTree {
    public final char value; // the character this leaf represents

    public HuffmanLeaf(int freq, char val) {
        super(freq);
        value = val;
    }
}

class HuffmanNode extends HuffmanTree {
    public final HuffmanTree left, right; // subtrees

    public HuffmanNode(HuffmanTree l, HuffmanTree r) {
        super(l.frequency + r.frequency);
        left = l;
        right = r;
    }
}

class HuffmanEncoder {
    private static Map<Character, String> map = new HashMap<Character, String>();

    private static HuffmanTree buildTree(int[] charFreqs) {
        PriorityQueue<HuffmanTree> trees = new PriorityQueue<HuffmanTree>();
        // initially, we have a forest of leaves
        // one for each non-empty character
        for (int i = 0; i < charFreqs.length; i++)
            if (charFreqs[i] > 0)
                trees.offer(new HuffmanLeaf(charFreqs[i], (char)i));

        assert trees.size() > 0;
        // loop until there is only one tree left
        while (trees.size() > 1) {
            // two trees with least frequency
            HuffmanTree a = trees.poll();
            HuffmanTree b = trees.poll();

            // put into new node and re-insert into queue
            trees.offer(new HuffmanNode(a, b));
        }
        return trees.poll();
    }

    private static void StoreCodes(HuffmanTree tree, String prefix) {
        assert tree != null;

        if(tree instanceof HuffmanLeaf) {
            HuffmanLeaf leaf = (HuffmanLeaf) tree;

            map.put(leaf.value, prefix);
        }
        else if(tree instanceof HuffmanNode) {
            HuffmanNode node = (HuffmanNode) tree;

            StoreCodes(node.left, prefix + '0');
            StoreCodes(node.right, prefix + '1');
        }
    }

    private static Data Encode(HuffmanTree tree, String str) {
        assert tree != null;
        //ret is a binary string. something like 010100101011010010101
        String ret = "";

        for(char c : str.toCharArray()) {
            ret += map.get(c);
        }

        char[] chars = new char[0];
        int offset = 0; // first couple of characters will be char map and how many bit we have encoded
        // that is why we need an offset

        if(ret.length() % 8 == 0) {
            chars = new char[ret.length() / 8 + offset];
        }
        else {
            chars = new char[ret.length() / 8 + 1 + offset];
        }

        for(int i = ret.length(); i > 0; i -= 8) {
            String s = ret.substring((i - 8 < 0) ? 0 : i - 8, i);
            int index = i / 8;

            for(char c : s.toCharArray()) {
                chars[index] <<= 1;
                if(c == '1') {
                    chars[index] |= 1;
                }
            }
        }

        //sending the length and the encoded string
        //encoded string is what we have build from binary string
        return new Data(new String(chars), ret.length());
    }

    public static Data Run (String to_encode) {
        String test = "A paragraph is a component of fictional prose and non-fiction writings. " +
                "When writing essays, research papers, books, etc., new paragraphs are indented to show their beginnings. " +
                "Each new paragraph begins with a new indentation. " +
                "The purpose of a paragraph is to express a speaker's thoughts on a particular point in a clear way " +
                "that is unique and specific to that paragraph. " +
                "In other words, paragraphs shouldn't be mixing thoughts or ideas. " +
                "When a new idea is introduced, generally, a writer will introduce a new paragraph.";

        int[] charFreqs = new int[256];
        // read each character and record the frequencies

        for (char c : to_encode.toCharArray())
            charFreqs[c]++;

        // build tree
        HuffmanTree tree = buildTree(charFreqs);
        StoreCodes(tree, "");

        Data ret = Encode(tree, to_encode);
        String encoded_string = ret.encoded_data;
        ret.freqs = charFreqs;

        Log.d("huffmancoder", "Original String: " + to_encode);
        Log.d("huffmancoder", "Original String Length: " + to_encode.length());

        Log.d("huffmancoder", "Encoded String: " + encoded_string);
        Log.d("huffmancoder", "Encoded String Length: " + encoded_string.length());

        return ret;
    }
}

class HuffmanDecoder {
    private static Map<Character, String> map = new HashMap<Character, String>();

    private static HuffmanTree buildTree(int[] charFreqs) {
        PriorityQueue<HuffmanTree> trees = new PriorityQueue<HuffmanTree>();
        // initially, we have a forest of leaves
        // one for each non-empty character
        for (int i = 0; i < charFreqs.length; i++)
            if (charFreqs[i] > 0)
                trees.offer(new HuffmanLeaf(charFreqs[i], (char)i));

        assert trees.size() > 0;
        // loop until there is only one tree left
        while (trees.size() > 1) {
            // two trees with least frequency
            HuffmanTree a = trees.poll();
            HuffmanTree b = trees.poll();

            // put into new node and re-insert into queue
            trees.offer(new HuffmanNode(a, b));
        }
        return trees.poll();
    }

    private static void StoreCodes(HuffmanTree tree, String prefix) {
        assert tree != null;

        if(tree instanceof HuffmanLeaf) {
            HuffmanLeaf leaf = (HuffmanLeaf) tree;

            map.put(leaf.value, prefix);
        }
        else if(tree instanceof HuffmanNode) {
            HuffmanNode node = (HuffmanNode) tree;

            StoreCodes(node.left, prefix + '0');
            StoreCodes(node.right, prefix + '1');
        }
    }

    private static String Decode(HuffmanTree tree, String str, int num) {
        assert tree != null;

        String binary = "";
        String ret = "";
        HuffmanTree current = tree;
        //binary will be a binary string that contains only 1 or 0
        //ret will be decoded string. human readable string

        for(int i = 0; i < str.length(); i++) {
            String part = "";
            char c = str.charAt(i);

            if(i == 0) {
                int x = num % 8;

                for(int j = 0; j < x; j++) {
                    part = ("" + ((int) c & 1)) + part;
                    c = (char) (((int) c) >> 1);
                }
            }
            else {
                for(int j = 0; j < 8; j++) {
                    part = ("" + ((int) c & 1)) + part;
                    c = (char) (((int) c) >> 1);
                }

            }

            binary = binary + part;
        }

        for(char c : binary.toCharArray()) {
            if(current instanceof HuffmanNode) {
                HuffmanNode current_node = (HuffmanNode) current;
                if(c == '0') {
                    current = current_node.left;
                }
                else if(c == '1') {
                    current = current_node.right;
                }

                if(current instanceof HuffmanLeaf) {
                    HuffmanLeaf current_leaf = (HuffmanLeaf) current;
                    ret += current_leaf.value;
                    current = tree;
                }
            }
        }

        return ret;
    }

    public static void Run(Data data) {

        // build tree
        HuffmanTree tree = buildTree(data.freqs);
        StoreCodes(tree, "");

        String decoded_string = Decode(tree, data.encoded_data, data.num_of_bits);
        Log.d("huffmancoder", "Decoded String: " + decoded_string);
        Log.d("huffmancoder", "Decoded String Length: " + decoded_string.length());
    }
}

public class MainActivity extends AppCompatActivity {
    private Socket socket;
    private PrintWriter out;


    public static final int FILE_SELECT_CODE = 0;
    ListView listView;
    String username;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(MainActivity.this,
                android.R.layout.simple_list_item_1,
                getResources().getStringArray(R.array.users));
        listView = (ListView) findViewById(R.id.listview1);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // TODO: action when item clicked
            }
        });
        listView.setAdapter(mAdapter);
        username = getIntent().getStringExtra("EXTRA_USERNAME");
        Log.d("user",username);

        //HuffmanEncoder returns Data: (String: encoded string, int[]: character frequencies, int: number of bits we need to read)
        //HuffmanDecoder needs Data: (String: a string to decode, int[]: character frequencies, int: number of bits we are going to read)
        //HuffmanDecoder.Run(HuffmanEncoder.Run(""));

        String host = "192.168.2.106";
        int port = 12345;

        new Thread(new ClientThread()).start();
    }

    class ClientThread implements Runnable {
        @Override
        public void run() {
            try {
                socket = new Socket("192.168.2.106", 12345);
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

                out.println(username);
                out.flush();

                while(true) {
                    //start reading
                }
            }
            catch(UnknownHostException e1) {
                e1.printStackTrace();
            }
            catch(IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    @Override
    protected void onActivityResult(int requst_code, int result_code, Intent data) {
        String fileContent;
        switch (requst_code) {
            case FILE_SELECT_CODE:
                if(result_code == RESULT_OK) {
                    Uri uri = data.getData();
                    Log.d("fileinformation", "File Uri: " + uri.toString());

                    try {
                        fileContent = ReadFromFile(uri);
                        Log.d("fileinformation", fileContent);

                        out.println(HuffmanEncoder.Run(fileContent).encoded_data);
                        out.flush();
                    }
                    catch (FileNotFoundException ex) {
                        Log.d("fileinformation", "reading file: " + ex.toString());
                    }

                    /*try {
                        String path = FileUtils.GetPath(this, uri);
                        Log.d("fileinformation", "File path: " + path);
                    }
                    catch (URISyntaxException ex) {
                        Log.d("fileinformation", "getting file path: " + ex.toString());
                    }*/
                }
                break;
        }
        super.onActivityResult(requst_code, result_code, data);
    }

    public void Show(View v) {
        //String str = "simple string";
        //TextView view = (TextView) findViewById(R.id.mytext);
        //view.setText(str);
        //Button button = (Button) findViewById(R.id.button);
        //button.setText(str);
        ShowFileChooser();
    }

    private String ReadFromFile(Uri uri) throws FileNotFoundException {
        String path = "";
        try {
            path = FileUtils.GetPath(this, uri);
            Log.d("fileinformation", "File path: " + path);
        }
        catch (URISyntaxException ex) {
            Log.d("fileinformation", "getting file path: " + ex.toString());
        }

        File f = new File(path);
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            Log.d("fileinformation", e.toString());
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int i;
        try {
            i = inputStream.read();
            while (i != -1) {
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();
        } catch (IOException e) {
            Log.d("fileinformation", e.toString());
        }
        return byteArrayOutputStream.toString();
    }

    private void ShowFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
        }
        catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager", Toast.LENGTH_SHORT).show();
        }
    }
}
