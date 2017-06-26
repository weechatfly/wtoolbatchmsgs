package com.easy.wtool.sdk.demo.batchmsgs;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import com.easy.wtool.sdk.MessageEvent;
import com.easy.wtool.sdk.OnMessageListener;
import com.easy.wtool.sdk.OnTaskEndListener;
import com.easy.wtool.sdk.TaskEndEvent;
import com.easy.wtool.sdk.WToolSDK;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private static String LOG_TAG = "javahook";
    private static String DEF_TALKER = "接收人(点击选择)";
    private static String DEF_IMAGEFILE = "图片(点击选择)";
    private static String DEF_VOICEFILE = "语音(点击选择)";
    private static String DEF_VIDEOFILE = "视频(点击选择)";
    private static int RESULT_IMAGE = 1;
    private static int RESULT_VOICE = 2;
    private static int RESULT_VIDEO = 3;
    Context mContext;
    // Used to load the 'native-lib' library on application startup.

    private ConfigUtils configUtils;
    private Map<String,String> listToWxIds = new LinkedHashMap<String,String>();
    private int currentToIndex;


    private String toImageFile = "";
    private String toVoiceFile = "";
    private String toVideoFile = "";
    private String toVideoThumbFile = "";
    private List<String> selectedWxIdIndex = new ArrayList<String>();

    TextView labelImageFile,labelVoiceFile,labelVideoFile;
    private ListView listViewSelectDialog = null;
    private boolean [] checks;
    private AlertDialog selectDialog = null;
    private ProgressDialog dialogProgress;
    final WToolSDK wToolSDK = new WToolSDK();
    EditText editText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = MainActivity.this;


        this.setTitle(this.getTitle() + " - V" + wToolSDK.getVersion());

        configUtils = new ConfigUtils(this);
        wToolSDK.encodeValue("1");

        TextView textViewPrompt = (TextView) findViewById(R.id.textViewPrompt);
        textViewPrompt.setClickable(true);
        textViewPrompt.setMovementMethod(LinkMovementMethod.getInstance());
        String prompt = "<b>本软件基于<a href=\"http://repo.xposed.info/module/com.easy.wtool\">微控工具xp模块-开发版[微信(wechat)二次开发模块]</a>"
                +"开发，使用前请确认模块已经安装，模块最低版本：1.0.1.102[1.0.0.102-开发版]</b>";
        textViewPrompt.setText(Html.fromHtml(prompt));
        // Example of a call to a native method
        //TextView tv = (TextView) findViewById(R.id.sample_text);
        //tv.setText(stringFromJNI());
        Button buttonInit = (Button) findViewById(R.id.buttonInit);
        Button buttonText = (Button) findViewById(R.id.buttonText);
        Button buttonImage = (Button) findViewById(R.id.buttonImage);
        Button buttonVoice = (Button) findViewById(R.id.buttonVoice);
        Button buttonVideo = (Button) findViewById(R.id.buttonVideo);
        Button buttonFriends = (Button) findViewById(R.id.buttonFriends);
        buttonFriends.setVisibility(View.INVISIBLE);
        Button buttonChatrooms = (Button) findViewById(R.id.buttonChatrooms);
        buttonChatrooms.setVisibility(View.INVISIBLE);
        final RadioButton radioButtonFriend = (RadioButton)findViewById(R.id.radioButtonFriend);
        final RadioButton radioButtonChatroom = (RadioButton)findViewById(R.id.radioButtonChatroom);
        final RadioButton radioButtonAll = (RadioButton)findViewById(R.id.radioButtonAll);
        final TextView labelWxid = (TextView) findViewById(R.id.labelWxid);
        labelWxid.setText(DEF_TALKER);
        labelImageFile = (TextView) findViewById(R.id.labelImageFile);
        labelImageFile.setText(DEF_IMAGEFILE);
        labelVoiceFile = (TextView) findViewById(R.id.labelVoiceFile);
        labelVoiceFile.setText(DEF_VOICEFILE);
        labelVideoFile = (TextView) findViewById(R.id.labelVideoFile);
        labelVideoFile.setText(DEF_VIDEOFILE);
        final Button buttonStartMessage = (Button) findViewById(R.id.buttonStartMessage);
        buttonStartMessage.setVisibility(View.INVISIBLE);
        final EditText editAppId = (EditText) findViewById(R.id.editAppId);
        final EditText editAuthCode = (EditText) findViewById(R.id.editAuthCode);

        editText = (EditText) findViewById(R.id.editText);

        final TextView editContent = (TextView) findViewById(R.id.editContent);
        editAppId.setText(configUtils.get(ConfigUtils.KEY_APPID, ""));
        editAuthCode.setText(configUtils.get(ConfigUtils.KEY_AUTHCODE, ""));
        if (!editAuthCode.getText().toString().equals("")) {
            //初始化
            parseResult(wToolSDK.init(editAppId.getText().toString(),editAuthCode.getText().toString()));
        }


        editContent.setMovementMethod(ScrollingMovementMethod.getInstance());
        //处理消息 回调的Handler
        final Handler messageHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {


                MessageEvent event = (MessageEvent) msg.obj;
                try{
                    String conent = event.getContent();
                    JSONObject jsonObject = new JSONObject(conent);
                    editContent.append("handleMessage message: " + event.getTalker() + ","+event.getMsgType()+"," +wToolSDK.decodeValue(jsonObject.getString("content")) + "\n");
                }
                catch(Exception e)
                {
                    Log.e(LOG_TAG,"handleMessage message err",e);
                }

                super.handleMessage(msg);
            }
        };
        wToolSDK.setOnMessageListener(new OnMessageListener() {
            @Override
            public void messageEvent(MessageEvent event) {
                Log.d(LOG_TAG, "messageEvent on message: " + event.getTalker() + "," + event.getContent());

                //editContent.setText("message: "+event.getTalker()+","+event.getContent());
                //由于该回调是在线程中，因些如果是有UI更新，需要使用Handler
                messageHandler.obtainMessage(0, event).sendToTarget();

            }
        });
        //处理taskend回调的Handler
        final Handler taskEndHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {


                TaskEndEvent event = (TaskEndEvent) msg.obj;
                String content = (event.getContent());


                editContent.append("on taskend text: " + event.getType()+","+event.getTaskId()+","+ content + "\n");
                if(event.getType()==1||event.getType()==2||event.getType()==3||event.getType()==4) {
                    try {
                        JSONObject jsonObject = new JSONObject(content);
                        if (jsonObject.getInt("result") != 0) {
                            //fail
                            currentToIndex = listToWxIds.size();
                            dialogProgress.cancel();
                            Toast.makeText(mContext, jsonObject.getString("errmsg"), Toast.LENGTH_LONG).show();
                        }
                        else {
                            sendSDKMessage(event.getType());
                        }
                    } catch (Exception e) {

                    }

                }
                super.handleMessage(msg);
            }
        };
        wToolSDK.setOnTaskEndListener(new OnTaskEndListener() {
            @Override
            public void taskEndEvent(TaskEndEvent event) {
                String content = (event.getContent());
                String msgId = "";
                try {
                    JSONObject jsonObject = new JSONObject(content);
                    content = wToolSDK.decodeValue(jsonObject.getString("content"));

                }
                catch (Exception e)
                {

                }

                Log.d(LOG_TAG, "on task end: " +event.getType()+","+event.getTaskId()+","+ content + "\n");

                //editContent.setText("message: "+event.getTalker()+","+event.getContent());
                //由于该回调是在线程中，因些如果是有UI更新，需要使用Handler
                taskEndHandler.obtainMessage(0, event).sendToTarget();



            }
        });
        buttonInit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editAppId.getText().toString().equals("")) {
                    Toast.makeText(mContext, "AppId不能为空！", Toast.LENGTH_LONG).show();
                    return;
                }
                if (editAuthCode.getText().toString().equals("")) {
                    Toast.makeText(mContext, "授权码不能为空！", Toast.LENGTH_LONG).show();
                    return;
                }
                //初始化
                parseResult(wToolSDK.init(editAppId.getText().toString(),editAuthCode.getText().toString()));
                configUtils.save(ConfigUtils.KEY_APPID, editAppId.getText().toString());
                configUtils.save(ConfigUtils.KEY_AUTHCODE, editAuthCode.getText().toString());
            }
        });
        labelWxid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                //builder.setIcon(R.drawable.ic_launcher);
                builder.setTitle(radioButtonAll.isChecked()?"选择联系人":(radioButtonFriend.isChecked()? "选择接收好友":"选择接收群"));
                String content = "";
                String text = "";
                try {
                    final JSONArray jsonArray = new JSONArray();
                    boolean bfriends = radioButtonFriend.isChecked() || radioButtonAll.isChecked();
                    boolean bchatrooms = radioButtonChatroom.isChecked() || radioButtonAll.isChecked();
                    if(bfriends)
                    {
                        try {
                            JSONObject jsonTask = new JSONObject();
                            jsonTask.put("type", 5);
                            jsonTask.put("taskid", System.currentTimeMillis());
                            jsonTask.put("content", new JSONObject());
                            jsonTask.getJSONObject("content").put("pageindex", 0);
                            jsonTask.getJSONObject("content").put("pagecount", 0);

                            content = wToolSDK.sendTask(jsonTask.toString());
                        }
                        catch(Exception e)
                        {

                        }
                        final JSONObject jsonObject = new JSONObject(content);
                        if (jsonObject.getInt("result") == 0) {

                            JSONArray jsonArray1 = jsonObject.getJSONArray("content");
                            if (jsonArray1.length() > 0) {
                                for(int i=0;i<jsonArray1.length();i++) {
                                    jsonArray.put(jsonArray1.getJSONObject(i));
                                }
                            }
                        }
                        else
                        {
                            text = jsonObject.getString("errmsg");
                        }
                    }
                    if(bchatrooms && text.length()==0)
                    {
                        try {
                            JSONObject jsonTask = new JSONObject();
                            jsonTask.put("type", 6);
                            jsonTask.put("taskid", System.currentTimeMillis());
                            jsonTask.put("content", new JSONObject());
                            jsonTask.getJSONObject("content").put("pageindex", 0);
                            jsonTask.getJSONObject("content").put("pagecount", 0);
                            jsonTask.getJSONObject("content").put("ismembers", 0);
                            content = wToolSDK.sendTask(jsonTask.toString());
                        }
                        catch(Exception e)
                        {

                        }
                        final JSONObject jsonObject = new JSONObject(content);
                        if (jsonObject.getInt("result") == 0) {

                            JSONArray jsonArray1 = jsonObject.getJSONArray("content");
                            if (jsonArray1.length() > 0) {
                                for(int i=0;i<jsonArray1.length();i++) {
                                    jsonArray.put(jsonArray1.getJSONObject(i));
                                }
                            }
                        }
                        else
                        {
                            text = jsonObject.getString("errmsg");
                        }
                    }


                    if(text.length()==0) {
                        selectedWxIdIndex.clear();


                        if (jsonArray.length() > 0) {
                            final String[] friends = new String[jsonArray.length()+1];
                            friends[0] = "全选";
                            for (int i = 0; i < jsonArray.length(); i++) {
                                friends[i+1] = wToolSDK.decodeValue(jsonArray.getJSONObject(i).getString("nickname"));
                                if (friends[i+1].equals("")) {
                                    if (jsonArray.getJSONObject(i).has("displayname")) {
                                        friends[i+1] = wToolSDK.decodeValue(jsonArray.getJSONObject(i).getString("displayname"));
                                        if(friends[i+1].length()>20) {
                                            friends[i+1] = friends[i+1].substring(0, 20) + "...";
                                        }
                                    }
                                }
                                if(radioButtonAll.isChecked() && jsonArray.getJSONObject(i).has("displayname"))
                                {
                                    friends[i+1] = "(群)"+friends[i+1];
                                }
                            }

                            //    设置一个单项选择下拉框
                            /**
                             * 第一个参数指定我们要显示的一组下拉多选框的数据集合
                             * 第二个参数代表哪几个选项被选择，如果是null，则表示一个都不选择，如果希望指定哪一个多选选项框被选择，
                             * 需要传递一个boolean[]数组进去，其长度要和第一个参数的长度相同，例如 {true, false, false, true};
                             * 第三个参数给每一个多选项绑定一个监听器
                             */

                            checks = new boolean[jsonArray.length()+1];
                            //String ids = toWxId;
                            //if(toWxId.equals(""))
                            //{
                            //    ids = "[]";
                            //}
                            //ids = "[]";
                            //JSONArray jsonArray1 = new JSONArray(ids);
                            for(int i=0;i<checks.length;i++)
                            {
                                checks[i] = false;
                                /*
                                for(int j=0;j<jsonArray1.length();j++)
                                {
                                    if(jsonArray1.getString(j).equals(wToolSDK.decodeValue(jsonArray.getJSONObject(i).getString("wxid"))))
                                    {
                                        checks[i] = true;
                                        break;
                                    }
                                }
                                */
                            }
                            final DialogInterface.OnClickListener onOkClickListener = new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        listToWxIds.clear();
                                        if(selectedWxIdIndex.size()>0) {
                                            String nicknames = "";
                                            //toWxId = "[";
                                            for (int i = 0; i < selectedWxIdIndex.size(); i++) {
                                                if (i > 0) {
                                                    //toWxId += ",";
                                                    nicknames += ",";
                                                }
                                                //toWxId += "\"" + wToolSDK.decodeValue(jsonArray.getJSONObject(Integer.parseInt(selectedWxIdIndex.get(i))).getString("wxid")) + "\"";
                                                nicknames += friends[Integer.parseInt(selectedWxIdIndex.get(i))+1];
                                                listToWxIds.put(wToolSDK.decodeValue(jsonArray.getJSONObject(Integer.parseInt(selectedWxIdIndex.get(i))).getString("wxid")),friends[Integer.parseInt(selectedWxIdIndex.get(i))+1]);
                                            }
                                            if (nicknames.length() > 50) {
                                                nicknames = nicknames.substring(0, 50) + "...";
                                            }
                                            //toWxId += "]";
                                            labelWxid.setText(DEF_TALKER + "：" + nicknames);
                                        }
                                        else
                                        {
                                            //toWxId = "";
                                            listToWxIds.clear();
                                        }
                                    } catch (Exception e) {
                                        listToWxIds.clear();
                                    }

                                }
                            };
                            final DialogInterface.OnMultiChoiceClickListener onMultiChoiceClickListener = new DialogInterface.OnMultiChoiceClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which, boolean isChecked) {

                                    if(which==0)
                                    {
                                        if(isChecked) {
                                            SparseBooleanArray sb;
                                            sb = listViewSelectDialog.getCheckedItemPositions();
                                            for (int i = 1; i < checks.length; i++) {
                                                if (sb.get(i) == false) {
                                                    listViewSelectDialog.setItemChecked(i, isChecked);
                                                }


                                                if (!selectedWxIdIndex.contains(String.valueOf(i-1))) {
                                                    selectedWxIdIndex.add(String.valueOf(i-1));
                                                }


                                            }
                                        }
                                        else
                                        {
                                            SparseBooleanArray sb;
                                            sb = listViewSelectDialog.getCheckedItemPositions();
                                            for (int i = 1; i < checks.length; i++) {
                                                if (sb.get(i) == true) {
                                                    listViewSelectDialog.setItemChecked(i, isChecked);
                                                }
                                                if (selectedWxIdIndex.contains(String.valueOf(i-1))) {
                                                    selectedWxIdIndex.remove(String.valueOf(i-1));
                                                }
                                            }
                                            //下面这个必须加，不然如果是单独勾选的，全不选时取消不了
                                            for (int i = 0; i < checks.length; i++) {
                                                checks[i] = false;
                                            }
                                            listViewSelectDialog.clearChoices();
                                        }


                                    }
                                    else {
                                        checks[which] = isChecked;
                                        //Log.d(LOG_TAG,"select "+which+","+isChecked);
                                        if (isChecked) {

                                            if (!selectedWxIdIndex.contains(String.valueOf(which-1))) {
                                                selectedWxIdIndex.add(String.valueOf(which-1));
                                            }

                                        } else {

                                            if (selectedWxIdIndex.contains(String.valueOf(which-1))) {
                                                selectedWxIdIndex.remove(String.valueOf(which-1));
                                            }

                                        }
                                        SparseBooleanArray sb;
                                        sb = listViewSelectDialog.getCheckedItemPositions();
                                        if(selectedWxIdIndex.size()==jsonArray.length())
                                        {
                                            if (sb.get(0) == false) {
                                                listViewSelectDialog.setItemChecked(0, true);
                                            }
                                            checks[0] = true;
                                        }
                                        else if(selectedWxIdIndex.size()==0)
                                        {
                                            if (sb.get(0) == true) {
                                                listViewSelectDialog.setItemChecked(0, false);
                                            }
                                            checks[0] = false;
                                        }
                                    }
                                    //builder.setPositiveButton("确定", selectedWxIdIndex.size()>0?onOkClickListener:null);
                                    Button button = selectDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                                    if(button!=null)
                                    {
                                        button.setEnabled(selectedWxIdIndex.size()>0);
                                    }
                                }
                            };
                            builder.setMultiChoiceItems(friends, checks, onMultiChoiceClickListener);

                            builder.setPositiveButton("确定", onOkClickListener);
                            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                            //builder.show();
                            selectDialog = builder.create();
                            selectDialog.show();
                            listViewSelectDialog = selectDialog.getListView();

                            Button button = selectDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                            if(button!=null)
                            {
                                button.setEnabled(false);
                            }

                        } else {
                            text = "无联系人";
                        }
                    }


                } catch (Exception e) {
                    text = "解析结果失败>>"+e.getMessage();
                    Log.e(LOG_TAG, "jsonerr", e);
                }
                if (text.length() > 0) {
                    Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
                }


            }

        });
        buttonText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listToWxIds.isEmpty()) {
                    Toast.makeText(mContext, "请选择接收人！", Toast.LENGTH_LONG).show();
                    return;
                }
                if (editText.getText().toString().equals("")) {
                    Toast.makeText(mContext, "发送内容不能为空！", Toast.LENGTH_LONG).show();
                    return;
                }
                //发送文本
                showProgressDialog();
                currentToIndex = 0;
                sendSDKMessage(1);

            }
        });
        labelImageFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent();//Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, RESULT_IMAGE);
            }
        });
        buttonImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listToWxIds.isEmpty()) {
                    Toast.makeText(mContext, "请选择接收人！", Toast.LENGTH_LONG).show();
                    return;
                }
                if (toImageFile.equals("")) {
                    Toast.makeText(mContext, "请选择要发送的图片！", Toast.LENGTH_LONG).show();
                    return;
                }
                //发送图片
                showProgressDialog();
                currentToIndex = 0;
                sendSDKMessage(2);


            }
        });
        labelVoiceFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent();//Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("audio/*");

                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, RESULT_VOICE);
            }
        });
        buttonVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listToWxIds.isEmpty()) {
                    Toast.makeText(mContext, "请选择接收人！", Toast.LENGTH_LONG).show();
                    return;
                }
                if (toVoiceFile.equals("")) {
                    Toast.makeText(mContext, "请选择要发送的语音文件！", Toast.LENGTH_LONG).show();
                    return;
                }
                //发送语音
                showProgressDialog();
                currentToIndex = 0;
                sendSDKMessage(3);




            }
        });
        labelVideoFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent();//Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setType("video/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, RESULT_VIDEO);
            }
        });
        buttonVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listToWxIds.isEmpty()) {
                    Toast.makeText(mContext, "请选择接收人！", Toast.LENGTH_LONG).show();
                    return;
                }
                if (toVideoFile.equals("")) {
                    Toast.makeText(mContext, "请选择要发送的视频文件！", Toast.LENGTH_LONG).show();
                    return;
                }
                //发送视频
                toVideoThumbFile = makeVideoThumbFile(toVideoFile);
                if(toVideoThumbFile.equals(""))
                {
                    Toast.makeText(mContext, "生成视频缩略图失败！", Toast.LENGTH_LONG).show();
                    return;
                }
                showProgressDialog();
                currentToIndex = 0;
                sendSDKMessage(4);

            }
        });

        buttonStartMessage.setTag(0);

        buttonStartMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonStartMessage.getTag().equals(0)) {
                    try {
                        JSONObject jsonObject = new JSONObject();
                        JSONArray jsonArray = new JSONArray();
                        jsonArray.put(1);
                        jsonArray.put(2);
                        jsonObject.put("talkertypes", jsonArray);
                        jsonObject.put("froms", new JSONArray());
                        jsonArray = new JSONArray();
                        jsonArray.put(1);
                        jsonArray.put(42);
                        jsonObject.put("msgtypes", jsonArray);
                        jsonObject.put("msgfilters", new JSONArray());
                        String result = wToolSDK.startMessageListener(jsonObject.toString());
                        jsonObject = new JSONObject(result);
                        if (jsonObject.getInt("result") == 0) {
                            buttonStartMessage.setTag(1);
                            buttonStartMessage.setText("停止监听消息");
                        }
                    } catch (Exception e) {
                        Log.e(LOG_TAG, "err", e);
                    }
                } else {
                    wToolSDK.stopMessageListener();
                    buttonStartMessage.setTag(0);
                    buttonStartMessage.setText("监听消息");
                }
            }
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && data != null) {
            String v_path = "";
            /*
            Cursor cursor = null;
            try {
                try {

                    Uri uri = data.getData();
                    cursor = getContentResolver().query(uri, null, null,
                            null, null);
                    cursor.moveToFirst();
                    // String imgNo = cursor.getString(0); // 图片编号
                    v_path = cursor.getString(1); // 图片文件路径
                    //String v_size = cursor.getString(2); // 图片大小
                    //String v_name = cursor.getString(3); // 图片文件名
                    cursor.close();


                } catch (Exception e) {
                    //e.printStackTrace();
                    //Toast.makeText(mContext, "获取文件出错！", Toast.LENGTH_LONG).show();
                    try
                    {
                        Uri uri = data.getData();//得到uri，后面就是将uri转化成file的过程。
                        v_path = uri.getPath().toString();
                    }
                    catch (Exception e1)
                    {
                        Toast.makeText(mContext, "获取文件出错！", Toast.LENGTH_LONG).show();
                        return;
                    }

                }
            }
            finally {
                if(cursor!=null)
                {
                    try {
                        cursor.close();
                    }
                    catch (Exception e)
                    {

                    }
                }
            }
            */
            try
            {
                v_path = UriUtils.getPath(mContext,data.getData());
                if(v_path==null)
                {
                    v_path = "";
                }
            }
            catch (Exception e)
            {
                Toast.makeText(mContext, "error>>"+e.getMessage(), Toast.LENGTH_LONG).show();
            }
            if(v_path.length()>0) {
                if (requestCode == RESULT_IMAGE) {

                /*
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};

                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String picturePath = cursor.getString(columnIndex);
                cursor.close();
                labelImageFile.setText(DEF_IMAGEFILE + "：" + picturePath);
                toImageFile = picturePath;
                */

                    //LogUtil.e("v_path="+v_path);
                    //LogUtil.e("v_size="+v_size);
                    //LogUtil.e("v_name="+v_name);
                    toImageFile = v_path;
                    labelImageFile.setText(DEF_IMAGEFILE + "：" + toImageFile);


                } else if (requestCode == RESULT_VOICE) {


                    toVoiceFile = v_path;
                    labelVoiceFile.setText(DEF_VOICEFILE + "：" + toVoiceFile);


                } else if (requestCode == RESULT_VIDEO) {


                    toVideoFile = v_path;
                    labelVideoFile.setText(DEF_VIDEOFILE + "：" + toVideoFile);


                }
            }
            else
            {
                Toast.makeText(mContext, "无法获取选择的文件路径", Toast.LENGTH_LONG).show();
            }
        }
    }
    private void sendSDKMessage(int type)
    {
        if(currentToIndex<listToWxIds.size()) {
            if(currentToIndex==0)
            {
                dialogProgress.setMax(listToWxIds.size());

            }
            dialogProgress.setProgress(currentToIndex);
            String wxid = "",nickname = "";
            int index = 0;
            for(String key: listToWxIds.keySet())
            {
                if(index==currentToIndex)
                {
                    wxid = key;
                    nickname = listToWxIds.get(key);
                    break;
                }
                index++;
            }


            try {
                dialogProgress.setMessage("正在发送给："+nickname+"...");
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("type",type);
                jsonObject.put("taskid", System.currentTimeMillis());
                jsonObject.put("content", new JSONObject());
                jsonObject.getJSONObject("content").put("talker", wxid);
                jsonObject.getJSONObject("content").put("timeout",60);
                if(type==1)
                {
                    if(currentToIndex==0)
                    {
                        dialogProgress.setTitle("发送文本");//设置标题
                    }
                    //text
                    jsonObject.getJSONObject("content").put("text",wToolSDK.encodeValue(editText.getText().toString()));
                }
                else if(type==2)
                {
                    if(currentToIndex==0)
                    {
                        dialogProgress.setTitle("发送图片");//设置标题
                    }
                    //image
                    jsonObject.getJSONObject("content").put("imagefile",toImageFile);

                }
                else if(type==3) {
                    //voice
                    if(currentToIndex==0)
                    {
                        dialogProgress.setTitle("发送语音");//设置标题
                    }
                    jsonObject.getJSONObject("content").put("voicefile", toVoiceFile);
                    jsonObject.getJSONObject("content").put("duration", 60);
                }
                else if(type==4)
                {
                    //video
                    if(currentToIndex==0)
                    {
                        dialogProgress.setTitle("发送视频");//设置标题
                    }
                    jsonObject.getJSONObject("content").put("videofile",toVideoFile);
                    jsonObject.getJSONObject("content").put("videothumbfile", toVideoThumbFile);
                    jsonObject.getJSONObject("content").put("duration",60);
                }
                currentToIndex++;
                String result = wToolSDK.sendTask(jsonObject.toString());
                try {
                    JSONObject jsonObject1 = new JSONObject(result);
                    if (jsonObject1.getInt("result") == 0) {

                    } else {
                        dialogProgress.cancel();
                        currentToIndex = listToWxIds.size();
                        Toast.makeText(mContext, jsonObject1.getString("errmsg"), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    dialogProgress.cancel();
                    currentToIndex = listToWxIds.size();
                    Log.e(LOG_TAG,"sendVoice err",e);
                }
            } catch (Exception e) {
                dialogProgress.cancel();
                currentToIndex = listToWxIds.size();
                Log.e(LOG_TAG,"sendVoice err",e);
            }
        }
        else
        {
            dialogProgress.cancel();
            Toast.makeText(mContext, "发送结束", Toast.LENGTH_LONG).show();
        }

    }
    private void showProgressDialog()
    {
        dialogProgress = new ProgressDialog(mContext);
        //dialogProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);//设置风格为圆形进度条
        dialogProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialogProgress.setTitle("发送");//设置标题
        //dialogProgress.setIcon(R.drawable.icon);//设置图标
        dialogProgress.setMessage("正在发送中...");
        dialogProgress.setIndeterminate(false);//设置进度条是否为不明确
        dialogProgress.setCancelable(true);//设置进度条是否可以按退回键取消
        //dialogProgress.setButton("确定", new DialogInterface.OnClickListener(){

        //    @Override
        //    public void onClick(DialogInterface dialog, int which) {
        //        dialog.cancel();

        //    }

        //});
        dialogProgress.show();
    }
    private void parseResult(String result) {
        String text = "";
        try {
            JSONObject jsonObject = new JSONObject(result);
            if (jsonObject.getInt("result") == 0) {
                text = "操作成功";
            } else {
                text = jsonObject.getString("errmsg");
            }
        } catch (Exception e) {
            text = "解析结果失败";
        }
        Toast.makeText(mContext, text, Toast.LENGTH_LONG).show();
    }
    public static String makeVideoThumbFile(String videoFile)
    {
        File f = new File(videoFile+".jpg");
        if(f.exists())
        {
            return f.toString();
        }
        Bitmap tmp = getVideoThumbnail(videoFile);
        if(tmp!=null)
        {
            File thumbfile = saveImage(tmp,videoFile+".jpg");
            if(thumbfile!=null) {
                return thumbfile.toString();
            }
            else
            {
                return "";
            }
        }
        else
        {
            return "";
        }
    }
    public static File saveImage(Bitmap bmp, String fileName) {
        File appDir = new File(Environment.getExternalStorageDirectory(), "Boohee");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        if(fileName==null || fileName.equals("")) {
            fileName = System.currentTimeMillis() + ".jpg";
        }
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }
    public static Bitmap getVideoThumbnail(String filePath) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            bitmap = retriever.getFrameAtTime();

            if(bitmap.getWidth()>320 || bitmap.getHeight()>200)
            {
                float scale = 1;
                if(bitmap.getWidth()>320)
                {
                    scale = (float)320/(float)bitmap.getWidth();
                }
                else
                {
                    scale = (float)200/(float)bitmap.getHeight();
                }
                Matrix matrix = new Matrix();

                matrix.postScale(scale,scale);

                Bitmap resizeBmp = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
                bitmap = resizeBmp;

            }
        }
        catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }
        finally {
            try {
                retriever.release();
            }
            catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        return bitmap;
    }

}
