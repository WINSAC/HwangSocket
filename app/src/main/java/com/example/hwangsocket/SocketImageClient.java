package com.example.hwangsocket;
/* Offloading energy & accuracy test*/

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Button;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileInputStream;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;



public class SocketImageClient extends AppCompatActivity {

    int count = 0;
    int length = 10240;
    private Socket socket;
    private static final int SERVERPORT = 8888;
    private static final String SERVER_IP = "192.168.1.100";
    private static final String TAG = "HaoxinImageMsg"; // filter
    private static final String TAG2 = "CPUfrequency"; // filter

    private TextView textView;
    //private Handler handler = null;
    Drawable HaoxinImage;
    Bitmap bitmap;

    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            ImageView HwangImageView = (ImageView) findViewById(R.id.HwangImageView);
            HaoxinImage = getResources().getDrawable(R.drawable.w);
            bitmap = ((BitmapDrawable) HaoxinImage).getBitmap();
            HwangImageView.setImageBitmap(bitmap);
        }
    };
/*
    Handler handler3 = new Handler(){
        @Override
        public void handleMessage(Message msg){
            //CPU frequency recording Thread
            textView = findViewById(R.id.textView);
            new Thread(){
                public void run(){
                    try{
                        while (true){
                            Thread.sleep(100);//unit: ms
                            Message msg = new Message();
                            msg.what = UPDATE;
                            handler2.sendMessage(msg);
                        }
                    }catch (InterruptedException e){
                        e.printStackTrace();
                    }
                }
            }.start();
        }
    };
*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_socket_image_client);
        /* Thread */
        new Thread(new HwangImageThread()).start();

        /* CPU frequency recording Thread */
        /*
        textView = findViewById(R.id.textView);
        new Thread(){
            public void run(){
                try{
                    while (true){
                        Thread.sleep(100);//unit: ms
                        Message msg = new Message();
                        msg.what = UPDATE;
                        handler2.sendMessage(msg);
                    }
                }catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }.start();
        */
    }

    private static final int UPDATE = 0;
    private int Frequency = 0;
    private Handler handler2 = new Handler(){
        public void handleMessage(Message msg){
            switch (msg.what){
                case UPDATE:
                    Frequency = getCurCPU();
                    Log.i(TAG2, "CPU frequency: " + Frequency);
                    appendLog(Integer.toString(Frequency),"/TestResults/TCP/Frequency.txt");
                    textView.setText(Integer.toString(Frequency));
                    break;
                default:
                    break;
            }
            super.handleMessage(msg);
        }
    };

    /** Get current CPU frequency **/
    private final static String CurPath = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";//保存当前CPU频率
    public static int getCurCPU(){
        int result = 0;
        FileReader fr = null;
        BufferedReader br = null;
        try{
            fr = new FileReader(CurPath);
            br = new BufferedReader(fr);
            String text = br.readLine();
            result = Integer.parseInt(text.trim());
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            if (fr != null)
                try{
                    fr.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            if (br != null)
                try{
                    br.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
        }
        return result;
    }

    public void TCP1(View view){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    final long StartTime = SystemClock.elapsedRealtime();
                    while(true){
                        count += 1;
                        int id = 1;
                        String path = Environment.getExternalStorageDirectory() + "/DCIM/images/" + id + ".jpg";
                        Bitmap bitmap2 = getImages(path);
                        Log.i(TAG, "Get bitmap: " + id);
                        final long StartTime2 = SystemClock.elapsedRealtime();
                        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        DataOutputStream dataOS2 = new DataOutputStream(socket.getOutputStream());
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        Log.i(TAG, "Setup ...");
                        bitmap2.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                        final long StartTime3 = SystemClock.elapsedRealtime();
                        byte[] b = baos.toByteArray();
                        dataOS2.writeInt(b.length);
                        Log.i(TAG, "image size is" + b.length);
                        dataOS2.write(b);
                        Log.i(TAG, "Complete offloading frameID is" + id);
                        String rp = br.readLine() + System.getProperty("line.separator");
                        Log.i(TAG, "Response:" + rp);
                        final long allLatency3 = SystemClock.elapsedRealtime() - StartTime3;
                        final long allLatency2 = SystemClock.elapsedRealtime() - StartTime2;
                        Log.i(TAG,id + " latency2: " + allLatency2 + " Latency3: " + allLatency3);
                        appendLog(Long.toString(allLatency2),"/TestResults/TCP/Latency.txt");
                        //appendLog(Long.toString(allLatency3));
                    }
                    //final long AllLatency = SystemClock.elapsedRealtime() - StartTime;
                    //Log.i(TAG, "AllLatency: " + AllLatency);
                    /*
                    for (int id = 1; id < 7; id++) {
                        String path = Environment.getExternalStorageDirectory() + "/DCIM/images/" + id + ".jpg";
                        Bitmap bitmap2 = getImages(path);
                        Log.i(TAG, "Get bitmap: " + id);

                        final long StartTime = SystemClock.elapsedRealtime();
                        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        DataOutputStream dataOS2 = new DataOutputStream(socket.getOutputStream());
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        Log.i(TAG, "Setup ...");

                        bitmap2.compress(Bitmap.CompressFormat.JPEG, 50, baos);
                        byte[] b = baos.toByteArray();

                        dataOS2.writeInt(b.length);
                        Log.i(TAG, "image size is" + b.length);
                        dataOS2.write(b);
                        Log.i(TAG, "Complete offloading frameID is" + id);

                        String rp = br.readLine() + System.getProperty("line.separator");
                        Log.i(TAG, "Response:" + rp);

                        final long allLatency = SystemClock.elapsedRealtime() - StartTime;
                        Log.i(TAG,id + " Total latency: " + allLatency);
                        //Save latency to file
                        //appendLog(Long.toString(allLatency));
                    }*/

                }catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                handler.sendEmptyMessage(0);
            }
        };
        Thread hThread = new Thread(r);
        hThread.start();
    }

    public void TCP2(View view){
        Runnable r = new Runnable() {
            @Override
            public void run() {
                int size;
                try {
                    final long StartTime = SystemClock.elapsedRealtime();
                    while(count<100){
                        count += 1;
                        int id = 1;
                        /** Read an image **/
                        final long ReadTime = SystemClock.elapsedRealtime();
                        String path = Environment.getExternalStorageDirectory() + "/DCIM/images/" + id + ".jpg";
                        FileInputStream fis = new FileInputStream(path);
                        final long readDelay = SystemClock.elapsedRealtime()-ReadTime;
                        size = fis.available();// image size
                        Log.i(TAG, "Image size:  " + size + " Read image latency: " + readDelay);

                        /** Send the image **/
                        final long SendTime = SystemClock.elapsedRealtime();
                        DataOutputStream dataOS = new DataOutputStream(socket.getOutputStream());
                        dataOS.writeInt(size);// Send the image size first
                        Log.i(TAG, "The image size is: " + size);
                        byte[] buf = new byte[length];
                        int len;
                        while ((len = fis.read(buf))!= -1) {
                            dataOS.write(buf, 0, len);
                            Log.i(TAG,"sending: " + len);
                        }
                        fis.close();
                        final long SeDelay = SystemClock.elapsedRealtime() - SendTime;
                        Log.i(TAG, "Complete sending frame: " + count + " Sending delay: " + SeDelay);

                        /** receive server feedback **/
                        final long RevTime = SystemClock.elapsedRealtime();
                        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String rp = br.readLine() + System.getProperty("line.separator");
                        Log.i(TAG, "Response:" + rp);
                        final long RevDelay = SystemClock.elapsedRealtime() - RevTime;
                        Log.i(TAG,"Receiving delay:  " + RevDelay);

                        final long framelatency = SystemClock.elapsedRealtime() - SendTime;
                        Log.i(TAG,"framelatency: " + framelatency);
                        //appendLog(Long.toString(framelatency));
                    }
                    final long AllLatency = SystemClock.elapsedRealtime()-StartTime;
                    Log.i(TAG, "Total latency: " + AllLatency);
                }catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                handler.sendEmptyMessage(0);
            }
        };
        Thread hThread = new Thread(r);
        hThread.start();
    }

    public void TCP3(View view){
        //handler3.sendEmptyMessage(0);
        Runnable r = new Runnable() {
            @Override
            public void run() {
                int size;
                try {
                    final long StartTime = SystemClock.elapsedRealtime();
                    //while(count<1000){
                    while(true){
                        count += 1;
                        int id = 4500;
                        /** Read an image **/
                        final long ReadTime = SystemClock.elapsedRealtime();
                        //String path = Environment.getExternalStorageDirectory() + "/DCIM/images/" + id + ".bmp";
                        String path = Environment.getExternalStorageDirectory() + "/DCIM/images/" + id + ".jpg";
                        FileInputStream fis = new FileInputStream(path);
                        final long readDelay = SystemClock.elapsedRealtime()-ReadTime;
                        size = fis.available();// image size
                        Log.i(TAG, "Image size:  " + size + " Read image latency: " + readDelay);

                        /** Send the image **/
                        final long SendTime = SystemClock.elapsedRealtime();
                        DataOutputStream dataOS = new DataOutputStream(socket.getOutputStream());
                        dataOS.writeInt(size);// Send the image size first
                        Log.i(TAG, "The image size is: " + size);

                        byte[] buf = new byte[size];
                        fis.read(buf);
                        dataOS.write(buf, 0, size);
                        fis.close();

                        final long SeDelay = SystemClock.elapsedRealtime() - SendTime;
                        Log.i(TAG, "Complete sending frame: " + count + " Sending delay: " + SeDelay);
                        /** receive server feedback **/
                        /*
                        final long RevTime = SystemClock.elapsedRealtime();

                        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                        String rp = br.readLine() + System.getProperty("line.separator");
                        Log.i(TAG, "Response:" + rp);
                        final long RevDelay = SystemClock.elapsedRealtime() - RevTime;
                        Log.i(TAG,"Receiving delay:  " + RevDelay);
                        final long framelatency = SystemClock.elapsedRealtime() - SendTime;
                        Log.i(TAG,"RTT: " + framelatency);
                        */
                        SystemClock.sleep(10000);
                        /** record **/
                        //appendLog(Long.toString(framelatency),"/TestResults/TCP/Latency.txt");
                    }
                    //final long AllLatency = SystemClock.elapsedRealtime()-StartTime;
                    //Log.i(TAG, "Total latency: " + AllLatency);
                }catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                handler.sendEmptyMessage(0);
            }
        };
        Thread hThread = new Thread(r);
        hThread.start();
    }

    protected Bitmap getImages(String path){
        File mfile = new File(path);
        if (mfile.exists()){
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            Log.i(TAG, "Read image!");
            return bitmap;
        }else{
            Log.i(TAG, "Path not exist!");
            return null;
        }
    }

    /* Define the Thread */
    class HwangImageThread implements Runnable{
        @Override
        public void run() {
            try{
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                Log.i(TAG,"New Socket");
                socket = new Socket(serverAddr, SERVERPORT);

            }catch (UnknownHostException e1){
                e1.printStackTrace();
            }catch (IOException e1){
                e1.printStackTrace();
            }
        }
    }

    /** Save Log to file **/
    public void appendLog(String text, String path)
    {
        final long timetest0 = SystemClock.elapsedRealtime();//
        File logFile = new File(Environment.getExternalStorageDirectory() + path);
        //File logFile = new File(Environment.getExternalStorageDirectory() + "/TestResults/UDPlatencyframe.txt");

        if (!logFile.exists())
        {
            Log.i(TAG," no this file");
            try
            {
                Log.i(TAG," create file ");
                logFile.createNewFile();
            }
            catch (IOException e)
            {
                //TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            //BufferedWriter for performance, true to set append to file flag
            //Log.i(TAG,"write........");
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.flush();
            buf.close();
            final long delay0 = SystemClock.elapsedRealtime() - timetest0;//
            //Log.i(TAG,"delay0: " + delay0);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /* Offload the Image
    public void OffloadImage(View view) throws IOException {

        try {
            ImageView HwangImageView = (ImageView) findViewById(R.id.HwangImageView);

            HaoxinImage = getResources().getDrawable(R.drawable.w);
            bitmap = ((BitmapDrawable) HaoxinImage).getBitmap();
            HwangImageView.setImageBitmap(bitmap);

            InputStream fis = getResources().openRawResource(+ R.drawable.h);
            //FileInputStream fis = new FileInputStream("w.jpg");
            Log.i(TAG,"InputStream");

            DataOutputStream dataOS = new DataOutputStream(socket.getOutputStream());

            byte[] buf = new byte[1024];
            int len;

            while ((len = fis.read(buf))!= -1) {
                dataOS.write(buf, 0, len);
                Log.i(TAG,"sending...");
            }
            Log.i(TAG,"Finish sending new image");


            String path = Environment.getExternalStorageDirectory() + "/HXW2/";
            File mImageFile = new File(path);
            Log.i(TAG, "2");
            if (!mImageFile.exists()) {
                mImageFile.mkdir();
                Log.i(TAG, "3");
            }
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String fileName = path + "IMG_" + timeStamp + ".jpg";
            Log.i(TAG, "4");
            final File file = new File(path, fileName);
            Log.i(TAG, "5");
            try {
                Log.i(TAG, "6");
                FileOutputStream out = new FileOutputStream(file);
                Log.i(TAG, "7");
                bitmap.compress(Bitmap.CompressFormat.PNG, 99, out);
                Log.i(TAG, "8");
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            //Log.i(TAG,"New Image");
            //ByteArrayOutputStream bytearrayOS = new ByteArrayOutputStream();

            //bitmap.compress(CompressFormat.PNG, 0, bytearrayOS);

            //byte[] array = bytearrayOS.toByteArray();

            //OutputStream outputstream = socket.getOutputStream();
            //DataOutputStream dataOS = new DataOutputStream(outputstream);

            //dataOS.writeInt(array.length);
            //dataOS.write(array, 0, array.length);

            //socket.shutdownOutput();

        }catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }*/
}
