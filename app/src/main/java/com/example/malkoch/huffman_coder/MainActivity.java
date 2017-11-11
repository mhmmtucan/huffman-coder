package com.example.malkoch.huffman_coder;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

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

class JSONUtils {
    public static Map<String, String> jsonToMap(JSONObject json) throws JSONException {
        Map<String, String> retMap = new HashMap<String, String>();

        if(json != JSONObject.NULL) {
            retMap = toMap(json);
        }
        return retMap;
    }

    public static List<String> jsonToList(JSONObject json) throws JSONException {
        List<String> retList = new ArrayList<String>();

        if(json != JSONObject.NULL) {
            retList = toList(json.getJSONArray("users"));
        }

        return retList;
    }

    private static Map<String, String> toMap(JSONObject object) throws JSONException {
        Map<String, String> map = new HashMap<String, String>();

        Iterator<String> keysItr = object.keys();
        while(keysItr.hasNext()) {
            String key = keysItr.next();
            Object value = object.get(key);

            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            map.put(key, value.toString());
        }
        return map;
    }

    private static List<String> toList(JSONArray array) throws JSONException {
        List<String> list = new ArrayList<String>();
        for(int i = 0; i < array.length(); i++) {
            Object value = array.get(i);
            if(value instanceof JSONArray) {
                value = toList((JSONArray) value);
            }

            else if(value instanceof JSONObject) {
                value = toMap((JSONObject) value);
            }
            list.add(value.toString());
        }
        return list;
    }
}

class Data {
    String encoded_data;
    int num_of_bits;
    int[] freqs;
    Map<String, String> map;

    public Data(String d, int b) {
        encoded_data = d;
        num_of_bits = b;
    }
}

abstract class HuffmanTree implements Comparable<HuffmanTree> {
    public final int frequency;
    public HuffmanTree(int freq) { frequency = freq; }

    public int compareTo(HuffmanTree tree) {
        return frequency - tree.frequency;
    }
}

class HuffmanLeaf extends HuffmanTree {
    public char value;

    public HuffmanLeaf(int freq, char val) {
        super(freq);
        value = val;
    }

    public HuffmanLeaf(char val) {
        super(1);
        value = val;
    }
}

class HuffmanNode extends HuffmanTree {
    public HuffmanTree left, right;

    public HuffmanNode(HuffmanTree l, HuffmanTree r) {
        super(l.frequency + r.frequency);
        left = l;
        right = r;
    }

    public HuffmanNode() {
        super(1);
        left = null;
        right = null;
    }
}

class HuffmanEncoder {
    static Map<String, String> map = new HashMap<String, String>();

    private static HuffmanTree buildTree(int[] charFreqs) {
        PriorityQueue<HuffmanTree> trees = new PriorityQueue<HuffmanTree>();
        for (int i = 0; i < charFreqs.length; i++)
            if (charFreqs[i] > 0)
                trees.offer(new HuffmanLeaf(charFreqs[i], (char)i));

        assert trees.size() > 0;
        while (trees.size() > 1) {
            HuffmanTree a = trees.poll();
            HuffmanTree b = trees.poll();

            trees.offer(new HuffmanNode(a, b));
        }
        return trees.poll();
    }

    private static void StoreCodes(HuffmanTree tree, String prefix) {
        assert tree != null;

        if(tree instanceof HuffmanLeaf) {
            HuffmanLeaf leaf = (HuffmanLeaf) tree;

            map.put("" + leaf.value, prefix);
        }
        else if(tree instanceof HuffmanNode) {
            HuffmanNode node = (HuffmanNode) tree;

            StoreCodes(node.left, prefix + '0');
            StoreCodes(node.right, prefix + '1');
        }
    }

    private static Data Encode(String str) {
        String ret = "";

        for(char c : str.toCharArray()) {
            ret += map.get("" + c);
        }

        System.out.println(ret);

        char[] chars = new char[0];
        int offset = 0;

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

        return new Data(new String(chars), ret.length());
    }

    public static Data Run (String to_encode) {
        //String test = "Lorem ipsem this is a sample file for trying huffmann encoder compresision asadas";

        int[] charFreqs = new int[256];

        for (char c : to_encode.toCharArray())
            charFreqs[c]++;

        HuffmanTree tree = buildTree(charFreqs);
        StoreCodes(tree, "");

        System.out.println("String: " + to_encode);
        System.out.println("String Length: "+ to_encode.length());

        Data ret = Encode(to_encode);
        String encoded_string = ret.encoded_data;
        ret.freqs = charFreqs;
        ret.map = map;

        System.out.println("Encoded String: " + encoded_string);
        System.out.println("Encoded String Length: " + encoded_string.length());

        return ret;
    }
}

class HuffmanDecoder {
    static Map<String, String> map = new HashMap<String, String>();

    private static HuffmanTree buildTree(Map<String, String> m) {
        map = m;

        HuffmanNode node = new HuffmanNode();

        for(Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            HuffmanNode current_node = node;

            for(int i = 0; i < value.length(); i++) {
                char c = value.toCharArray()[i];

                if(i == value.length() - 1) {
                    if(c == '0') {
                        current_node.left = new HuffmanLeaf(key.charAt(0));
                        //System.out.println(((HuffmanLeaf) current_node.left).value);
                    }
                    else if(c == '1') {
                        current_node.right = new HuffmanLeaf(key.charAt(0));
                        //System.out.println(((HuffmanLeaf) current_node.right).value);
                    }
                }
                else {
                    if(c == '0') {
                        if(current_node.left == null) {
                            current_node.left = new HuffmanNode();
                        }
                        current_node = (HuffmanNode) current_node.left;
                    }
                    else if(c == '1') {
                        if(current_node.right == null) {
                            current_node.right = new HuffmanNode();
                        }
                        current_node = (HuffmanNode) current_node.right;
                    }
                }
            }
        }

        return node;
    }

    private static String HuffmanDecode(HuffmanTree tree, String str, int num) {
        assert tree != null;

        String binary = "";
        String ret = "";
        HuffmanTree current = tree;

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

        System.out.println(binary);

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

    public static String Run(Data data) {
        HuffmanTree tree = buildTree(data.map);

        String decoded_string = HuffmanDecode(tree, data.encoded_data, data.num_of_bits);
        System.out.println("Decoded String: " + decoded_string);
        System.out.println("Decoded String Length: " + decoded_string.length());
        return decoded_string;
    }
}

public class MainActivity extends AppCompatActivity {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public static final int FILE_SELECT_CODE = 0;
    ListView listView;
    //private ItemAdapter item_list_adapter;
    //private List<Item> item_list = new ArrayList<Item>();
    String username;
    String reciever;
    List<String> usernames = new ArrayList<String>();
    String str;

    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int which) {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    //WriteToFile("/storage/sdcard0/encoded.txt", "sender: " + sender, "receiver: " + reciever, "map: " + map, "len: " + number, "encoded text: " + text);
                    WriteToFile("/storage/sdcard0/decoded.txt", str);
                    dialogInterface.dismiss();
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    dialogInterface.dismiss();
                    break;
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ShowActiveUsers(MainActivity.this);

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
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                out.println(username);
                out.flush();

                while(true) {
                    String response = in.readLine();
                    //Log.d("serverresponse", "response : " + response);

                    try {
                        JSONObject json_object = new JSONObject(response);

                        if(json_object.has("users")) {
                            //getting users
                            Log.d("serverresponse", "getting the userlist");
                            usernames = JSONUtils.jsonToList(json_object);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ShowActiveUsers(MainActivity.this);
                                }
                            });
                        }
                        else {
                            //getting data
                            Log.d("serverresponse", "getting the data");

                            String sender = json_object.getString("sender");
                            String reciever = json_object.getString("reciever");
                            String map = json_object.getString("map");
                            int number = json_object.getInt("len");
                            String text = json_object.getString("text");

                            JSONObject json_map = new JSONObject(map);
                            Map<String, String> m = JSONUtils.jsonToMap(json_map);

                            Log.d("serverresponse", "sender: " + sender);
                            Log.d("serverresponse", "reciever: " + reciever);
                            Log.d("serverresponse", "map: " + map);
                            Log.d("serverresponse", "number: " + number);
                            Log.d("serverresponse", "text: " + text);
                            Data d = new Data(text, number);
                            d.map = m;
                            String decoded_string = HuffmanDecoder.Run(d);
                            str = decoded_string;
                            Log.d("serverresponse", "decoded string: " + decoded_string);

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                    builder.setMessage("Are you sure");
                                    builder.setPositiveButton("Yes", dialogClickListener);
                                    builder.setNegativeButton("No", dialogClickListener);
                                    builder.show();
                                }
                            });

                            //WriteToFile("/storage/sdcard0/encoded.txt", "sender: " + sender, "receiver: " + reciever, "map: " + map, "len: " + number, "encoded text: " + text);
                            //WriteToFile("/storage/sdcard0/decoded.txt", "decoded text: " + decoded_string);
                        }
                    }
                    catch (JSONException e) {
                        e.printStackTrace();
                    }
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

                        //do jsonify
                        JSONObject json = new JSONObject();
                        try {
                            Data ret = HuffmanEncoder.Run(fileContent);
                            json.put("sender", username);
                            if(reciever != null && !reciever.equals("")) {
                                json.put("reciever", reciever);
                            }
                            else {
                                json.put("reciever", "server");
                            }

                            JSONObject j = new JSONObject(ret.map);
                            json.put("map", j.toString());

                            json.put("len", ret.num_of_bits);
                            json.put("text", ret.encoded_data);

                            Log.d("serverresponse", "encoded string: " + ret.encoded_data);
                            Log.d("serverresponse", "map: " + j.toString());
                            Log.d("serverresponse", "sending: " + json.toString());

                            out.println(json.toString());
                            out.flush();
                        }
                        catch(JSONException e) {
                            e.printStackTrace();
                        }

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

    public void Refresh(View v) {
        ShowActiveUsers(MainActivity.this);
    }

    public void ShowActiveUsers(Context context) {
        setContentView(R.layout.activity_main);
        ArrayList<String> users_list = new ArrayList<String>();
        for(String username : usernames) {
            if(!username.equals(this.username)) {
                users_list.add(username);
            }
        }

        ArrayAdapter<String> mAdapter = new ArrayAdapter<String>(context,
                android.R.layout.simple_list_item_1,
                users_list
        );

        listView = (ListView) findViewById(R.id.listview1);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                String name = listView.getAdapter().getItem(position).toString();
                reciever = name;
                Log.d("serverresponse", "reciever name: " + reciever);
                ShowFileChooser();
            }
        });

        listView.setAdapter(mAdapter);
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

    private void WriteToFile(String path, String... args) {
        File file = new File(path);

        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);
            for(String data : args) {
                pw.println(data);
                pw.flush();
            }
            pw.close();
            f.close();
        }
        catch(FileNotFoundException e) {
            e.printStackTrace();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
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