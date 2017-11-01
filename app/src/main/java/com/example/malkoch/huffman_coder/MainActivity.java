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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

class Client extends AsyncTask<String, Void, String> {
    @Override
    protected String doInBackground(String... args) {
        String hostname = args[0];
        int port_number = Integer.parseInt(args[1]);
        String data = args[2];

        String ret = "";

        try {
            Socket socket = new Socket(hostname, port_number);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true/*false*/);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            //while(!in.ready());
            String response = in.readLine();
            Log.d("client", response);

            //while(in.ready()) {
            //    ret = ret + (char) in.read();
            //}

            //out.write(data);
            out.println(data);
            //out.flush();
        }
        catch (Exception e) {
            System.out.print(e.toString());
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
                    //return cursor.getString(column_index);
                }
            }
            catch (Exception e) {
                Log.d("file", e.toString());
            }
        }
        else if("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
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

}

class HuffmanDecoder {

}

class HuffmanCode {
    static Map<Character, String> map = new HashMap<Character, String>();

    // input is an array of frequencies, indexed by character code
    public static HuffmanTree buildTree(int[] charFreqs) {
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

    public static void printCodes(HuffmanTree tree, StringBuffer prefix) {
        assert tree != null;

        if (tree instanceof HuffmanLeaf) {
            HuffmanLeaf leaf = (HuffmanLeaf)tree;

            // print out character, frequency, and code for this leaf (which is just the prefix)
            Log.d("huffman", leaf.value + "\t" + leaf.frequency + "\t" + prefix);

        } else if (tree instanceof HuffmanNode) {
            HuffmanNode node = (HuffmanNode)tree;

            // traverse left
            prefix.append('0');
            printCodes(node.left, prefix);
            prefix.deleteCharAt(prefix.length()-1);

            // traverse right
            prefix.append('1');
            printCodes(node.right, prefix);
            prefix.deleteCharAt(prefix.length()-1);
        }
    }

    public static void StoreCodes(HuffmanTree tree, String prefix) {
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

    public static String HuffmanEncode(HuffmanTree tree, String str) {
        assert tree != null;

        String ret = "";

        for(char c : str.toCharArray()) {
            ret += map.get(c);
        }

        Log.d("huffman", "len: " + ret.length());

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

        for(int i = 0; i < chars.length; i++) {
            Log.d("huffman", (int)chars[i] + " ");
        }

        System.out.println();

        for(int i = 0; i < chars.length; i++) {
            Log.d("huffman", "" + chars[i]);
        }

        System.out.println();

        return ret;
    }

    public static String HuffmanDecode(HuffmanTree tree, String str) {
        assert tree != null;

        String ret = "";
        HuffmanTree current = tree;

        for(char c : str.toCharArray()) {
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

    public static void Run() {
        String test = "A SIMPLE STRING TO BE ENCODED USING A MINIMAL NUMBER OF BITS";
        // we will assume that all our characters will have
        // code less than 256, for simplicity
        int[] charFreqs = new int[256];
        // read each character and record the frequencies
        for (char c : test.toCharArray())
            charFreqs[c]++;

        // build tree
        HuffmanTree tree = buildTree(charFreqs);
        StoreCodes(tree, "");

        // print out results
        Log.d("huffman", "SYMBOL\tWEIGHT\tHUFFMAN CODE");
        printCodes(tree, new StringBuffer());

        String encoded_string = HuffmanEncode(tree, test);
        Log.d("huffman", "encoded string: " + encoded_string);

        String decoded_string = HuffmanDecode(tree, encoded_string);
        Log.d("huffman", "decoded string: " + decoded_string);
    }
}

public class MainActivity extends AppCompatActivity {

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

        String host = "192.168.2.106";
        String port = "12345";
        Client client = new Client();
        client.execute(host, port, username);
    }

    @Override
    protected void onActivityResult(int requst_code, int result_code, Intent data) {
        String fileContent;
        switch (requst_code) {
            case FILE_SELECT_CODE:
                if(result_code == RESULT_OK) {
                    Uri uri = data.getData();
                    Log.d("file", "File Uri: " + uri.toString());
                    /*
                    try {
                        fileContent = ReadFromFile(uri);
                        Log.d("file", fileContent);
                    }
                    catch (FileNotFoundException ex) {
                        Log.d("file", ex.toString());
                    }
                    */
                    try {
                        String path = FileUtils.GetPath(this, uri);
                        Log.d("file", "File path: " + path);
                    }
                    catch (URISyntaxException ex) {
                        Log.d("file", ex.toString());
                    }
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
        HuffmanCode.Run();
        ShowFileChooser();
    }

    private String ReadFromFile(Uri uri) throws FileNotFoundException {

        File f = new File(String.valueOf(uri));
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
