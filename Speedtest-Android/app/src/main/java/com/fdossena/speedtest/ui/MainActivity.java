package com.fdossena.speedtest.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.fdossena.speedtest.core.Speedtest;
import com.fdossena.speedtest.core.config.SpeedtestConfig;
import com.fdossena.speedtest.core.config.TelemetryConfig;
import com.fdossena.speedtest.core.serverSelector.TestPoint;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import favosoft.speedtest.R;

public class MainActivity extends Activity {

    private SharedPreferences prefs;
    private static String SERVER_PREF_KEY = "server";
    private static String HIRTORY_PREF_KEY = "history";

    private static String SERVER_ADDR = "http://192.168.1.4:8866";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //transition(R.id.page_splash,0);
        new Thread(){
            public void run(){
//                try{sleep(1500);}catch (Throwable t){}
//                try {
//                    BitmapFactory.Options options = new BitmapFactory.Options();
//                    final ImageView v = (ImageView) findViewById(R.id.testBackground);
//                    options.inJustDecodeBounds = true;
//                    BitmapFactory.decodeResource(getResources(), R.drawable.testbackground, options);
//                    int ih = options.outHeight, iw = options.outWidth;
//                    if(4*ih*iw>16*1024*1024) throw new Exception("Too big");
//                    options.inJustDecodeBounds = false;
//                    DisplayMetrics displayMetrics = new DisplayMetrics();
//                    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
//                    int vh = displayMetrics.heightPixels, vw = displayMetrics.widthPixels;
//                    double desired=Math.max(vw,vh) * 0.7;
//                    double scale=desired/Math.max(iw,ih);
//                    final Bitmap b = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.testbackground, options),(int)(iw*scale), (int)(ih*scale), true);
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            v.setImageBitmap(b);
//                        }
//                    });
//                }catch (Throwable t){
//                    System.err.println("Failed to load testbackground ("+t.getMessage()+")");
//                }
                page_init();
            }
        }.start();
    }

    private static Speedtest st=null;

    private void page_init(){
        new Thread(){
            @Override
            public void run() {
                SpeedtestConfig config=null;
                TelemetryConfig telemetryConfig=null;
                TestPoint[] servers=null;
                try{
                    String c=readFileFromAssets("SpeedtestConfig.json");
                    JSONObject o=new JSONObject(c);
                    config=new SpeedtestConfig(o);
                    c=readFileFromAssets("TelemetryConfig.json");
                    o=new JSONObject(c);
                    telemetryConfig=new TelemetryConfig(o);
                    if(telemetryConfig.getTelemetryLevel().equals(TelemetryConfig.LEVEL_DISABLED)){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                hideView(R.id.privacy_open);
                            }
                        });
                    }
                    if(st!=null){
                        try{st.abort();}catch (Throwable e){}
                    }
                    st=new Speedtest();
                    st.setSpeedtestConfig(config);
                    st.setTelemetryConfig(telemetryConfig);
                    c=readFileFromAssets("ServerList.json");
                    if(c.startsWith("\"")||c.startsWith("'")){ //fetch server list from URL
                        if(!st.loadServerList(c.subSequence(1,c.length()-1).toString())){
                            throw new Exception("Failed to load server list");
                        }
                    }else{ //use provided server list
                        JSONArray a=new JSONArray(c);
                        if(a.length()==0) throw new Exception("No test points");
                        ArrayList<TestPoint> s=new ArrayList<>();
                        for(int i=0;i<a.length();i++) s.add(new TestPoint(a.getJSONObject(i)));
                        servers=s.toArray(new TestPoint[0]);
                        st.addTestPoints(servers);
                    }
                    final String testOrder=config.getTest_order();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(!testOrder.contains("D")){
                                hideView(R.id.dlArea);
                            }
                            if(!testOrder.contains("U")){
                                hideView(R.id.ulArea);
                            }
                            if(!testOrder.contains("P")){
                                hideView(R.id.pingArea);
                            }
                            if(!testOrder.contains("I")){
                                hideView(R.id.ipInfo);
                            }
                        }
                    });
                }catch (final Throwable e){
                    System.err.println(e);
                    st=null;
                    transition(R.id.page_fail,TRANSITION_LENGTH);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((TextView)findViewById(R.id.fail_text)).setText(getString(R.string.initFail_configError)+": "+e.getMessage());
                            final Button b=(Button)findViewById(R.id.fail_button);
                            b.setText(R.string.initFail_retry);
                            b.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    page_init();
                                    b.setOnClickListener(null);
                                }
                            });
                        }
                    });
                    return;
                }

                if (servers.length == 1) {
                    servers[0].setPing(0);
                    prefs = getPreferences(Context.MODE_PRIVATE);
                    SERVER_ADDR = prefs.getString(SERVER_PREF_KEY, SERVER_ADDR);
                    servers[0].setServer(SERVER_ADDR);
                    st.setSelectedServer(servers[0]);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            page_test(st.getSelectedServer());
                        }
                    });
                } else {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            transition(R.id.page_init,TRANSITION_LENGTH);
//                            final TextView t=((TextView)findViewById(R.id.init_text));
//                            t.setText(R.string.init_init);
//                            t.setText(R.string.init_selecting);
//                        }
//                    });
//                    st.selectServer(new Speedtest.ServerSelectedHandler() {
//                        @Override
//                        public void onServerSelected(final TestPoint server) {
//                            runOnUiThread(new Runnable() {
//                                @Override
//                                public void run() {
//                                    if (server == null) {
//                                        transition(R.id.page_fail, TRANSITION_LENGTH);
//                                        ((TextView) findViewById(R.id.fail_text)).setText(getString(R.string.initFail_noServers));
//                                        final Button b = (Button) findViewById(R.id.fail_button);
//                                        b.setText(R.string.initFail_retry);
//                                        b.setOnClickListener(new View.OnClickListener() {
//                                            @Override
//                                            public void onClick(View v) {
//                                                page_init();
//                                                b.setOnClickListener(null);
//                                            }
//                                        });
//                                    } else {
//                                        page_serverSelect(server, st.getTestPoints());
//                                    }
//                                }
//                            });
//                        }
//                    });
                }
            }
        }.start();
    }

//    private void page_serverSelect(TestPoint selected, TestPoint[] servers){
//        transition(R.id.page_serverSelect,TRANSITION_LENGTH);
//        reinitOnResume=true;
//        final ArrayList<TestPoint> availableServers=new ArrayList<>();
//        for(TestPoint t:servers) {
//            if (t.getPing() != -1) availableServers.add(t);
//        }
//        int selectedId=availableServers.indexOf(selected);
//        final Spinner spinner=(Spinner)findViewById(R.id.serverList);
//        ArrayList<String> options=new ArrayList<String>();
//        for(TestPoint t:availableServers){
//            options.add(t.getName());
//        }
//        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,options.toArray(new String[0]));
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinner.setAdapter(adapter);
//        spinner.setSelection(selectedId);
//        final Button b=(Button)findViewById(R.id.start);
//        b.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                reinitOnResume=false;
//                page_test(availableServers.get(spinner.getSelectedItemPosition()));
//                b.setOnClickListener(null);
//            }
//        });
//        TextView t=(TextView)findViewById(R.id.privacy_open);
//        t.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                page_privacy();
//            }
//        });
//    }

//    private void page_privacy(){
//        transition(R.id.page_privacy,TRANSITION_LENGTH);
//        reinitOnResume=false;
//        ((WebView)findViewById(R.id.privacy_policy)).loadUrl(getString(R.string.privacy_policy));
//        TextView t=(TextView)findViewById(R.id.privacy_close);
//        t.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                transition(R.id.page_serverSelect,TRANSITION_LENGTH);
//                reinitOnResume=true;
//            }
//        });
//    }

    private void page_test(final TestPoint selected){
        transition(R.id.page_test,TRANSITION_LENGTH);
        st.setSelectedServer(selected);
        ((TextView)findViewById(R.id.serverName)).setText("Test Server:" + "\n" + selected.getServerIp());
        ((TextView)findViewById(R.id.dlText)).setText(format(0));
        ((TextView)findViewById(R.id.ulText)).setText(format(0));
        ((TextView)findViewById(R.id.pingText)).setText(format(0));
        ((TextView)findViewById(R.id.jitterText)).setText(format(0));
        ((ProgressBar)findViewById(R.id.dlProgress)).setProgress(0);
        ((ProgressBar)findViewById(R.id.ulProgress)).setProgress(0);
        ((GaugeView)findViewById(R.id.dlGauge)).setValue(0);
        ((GaugeView)findViewById(R.id.ulGauge)).setValue(0);
        ((TextView)findViewById(R.id.ipInfo)).setText("");
        ((ImageView)findViewById(R.id.logo_inapp)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url=getString(R.string.logo_inapp_link);
                if(url.isEmpty()) return;
                Intent i=new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                startActivity(i);
            }
        });
        final View endTestArea=findViewById(R.id.endTestArea);
        final int endTestAreaHeight=endTestArea.getHeight();
        ViewGroup.LayoutParams p=endTestArea.getLayoutParams();
        p.height=0;
        endTestArea.setLayoutParams(p);
        //findViewById(R.id.setting_button).setVisibility(View.GONE);
        final Button setting_button=(Button)findViewById(R.id.setting_button);
        setting_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setTestServer();
            }
        });
        final Button history_button=(Button)findViewById(R.id.history_button);
        history_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showHistoryResult();
            }
        });
        st.start(new Speedtest.SpeedtestHandler() {
            @Override
            public void onDownloadUpdate(final double dl, final double progress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView)findViewById(R.id.dlText)).setText(progress==0?"...": format(dl));
                        ((GaugeView)findViewById(R.id.dlGauge)).setValue(progress==0?0:mbpsToGauge(dl));
                        ((ProgressBar)findViewById(R.id.dlProgress)).setProgress((int)(100*progress));
                    }
                });
            }

            @Override
            public void onUploadUpdate(final double ul, final double progress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView)findViewById(R.id.ulText)).setText(progress==0?"...": format(ul));
                        ((GaugeView)findViewById(R.id.ulGauge)).setValue(progress==0?0:mbpsToGauge(ul));
                        ((ProgressBar)findViewById(R.id.ulProgress)).setProgress((int)(100*progress));
                    }
                });

            }

            @Override
            public void onPingJitterUpdate(final double ping, final double jitter, final double progress) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView)findViewById(R.id.pingText)).setText(progress==0?"...": format(ping));
                        ((TextView)findViewById(R.id.jitterText)).setText(progress==0?"...": format(jitter));
                        ((TextView)findViewById(R.id.pingMs)).setVisibility(View.VISIBLE);
                        ((TextView)findViewById(R.id.jitterMs)).setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onIPInfoUpdate(final String ipInfo) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView)findViewById(R.id.ipInfo)).setText(ipInfo);
                    }
                });
            }

            @Override
            public void onTestIDReceived(final String id, final String shareURL) {
                if(shareURL==null||shareURL.isEmpty()||id==null||id.isEmpty()) return;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
//                        final Button setting_button=(Button)findViewById(R.id.setting_button);
//                        setting_button.setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                setTestServer();
//                            }
//                        });
                    }
                });
            }

            @Override
            public void onEnd() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final Button restartButton=(Button)findViewById(R.id.restartButton);
                        restartButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                page_init();
                                restartButton.setOnClickListener(null);
                            }
                        });
                    }
                });
                final long startT=System.currentTimeMillis(), endT=startT+TRANSITION_LENGTH;
                new Thread(){
                    public void run(){
                        while(System.currentTimeMillis()<endT){
                            final double f=(double)(System.currentTimeMillis()-startT)/(double)(endT-startT);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ViewGroup.LayoutParams p=endTestArea.getLayoutParams();
                                    p.height=(int)(endTestAreaHeight*f);
                                    endTestArea.setLayoutParams(p);
                                }
                            });
                            try{sleep(10);}catch (Throwable t){}
                        }
                    }
                }.start();

                saveTestResult();
            }

            @Override
            public void onCriticalFailure(String err) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView)findViewById(R.id.pingText)).setText("Error");
                        ((TextView)findViewById(R.id.jitterText)).setText("Error");
                        ((TextView)findViewById(R.id.pingMs)).setVisibility(View.GONE);
                        ((TextView)findViewById(R.id.jitterMs)).setVisibility(View.GONE);
                        ViewGroup.LayoutParams p=endTestArea.getLayoutParams();
                        p.height=150;
                        endTestArea.setLayoutParams(p);
                    }
                });
            }
        });
    }

    private String format(double d){
        Locale l=null;
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.N) {
            l = getResources().getConfiguration().getLocales().get(0);
        }else{
            l=getResources().getConfiguration().locale;
        }
        if(d<10) return String.format(l,"%.2f",d);
        if(d<100) return String.format(l,"%.1f",d);
        return ""+Math.round(d);
    }

    private int mbpsToGauge(double s){
        return (int)(1000*(1-(1/(Math.pow(1.3,Math.sqrt(s))))));
    }

    private String readFileFromAssets(String name) throws Exception{
        BufferedReader b=new BufferedReader(new InputStreamReader(getAssets().open(name)));
        String ret="";
        try{
            for(;;){
                String s=b.readLine();
                if(s==null) break;
                ret+=s;
            }
        }catch(EOFException e){}
        return ret;
    }

    private void hideView(int id){
        View v=findViewById(id);
        if(v!=null) v.setVisibility(View.GONE);
    }

    private boolean reinitOnResume=false;
    @Override
    protected void onResume() {
        super.onResume();
        if(reinitOnResume){
            reinitOnResume=false;
            page_init();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try{
            st.abort();
        } catch (Throwable t){

        }
    }
//
//    @Override
//    public void onBackPressed() {
//        if(currentPage==R.id.page_privacy)
//            transition(R.id.page_serverSelect,TRANSITION_LENGTH);
//        else super.onBackPressed();
//    }

    //PAGE TRANSITION SYSTEM

    private int currentPage=-1;
    private boolean transitionBusy=false; //TODO: improve mutex
    private int TRANSITION_LENGTH=300;

    private void transition(final int page, final int duration){
        if(transitionBusy){
            new Thread(){
                public void run(){
                    try{sleep(10);}catch (Throwable t){}
                    transition(page,duration);
                }
            }.start();
        }else transitionBusy=true;
        if(page==currentPage) return;
        final ViewGroup oldPage=currentPage==-1?null:(ViewGroup)findViewById(currentPage),
                newPage=page==-1?null:(ViewGroup)findViewById(page);
        new Thread(){
            public void run(){
                long t=System.currentTimeMillis(), endT=t+duration;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(newPage!=null){
                            newPage.setAlpha(0);
                            newPage.setVisibility(View.VISIBLE);
                        }
                        if(oldPage!=null){
                            oldPage.setAlpha(1);
                        }
                    }
                });
                while(t<endT){
                    t=System.currentTimeMillis();
                    final float f=(float)(endT-t)/(float)duration;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(newPage!=null) newPage.setAlpha(1-f);
                            if(oldPage!=null) oldPage.setAlpha(f);
                        }
                    });
                    try{sleep(10);}catch (Throwable e){}
                }
                currentPage=page;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(oldPage!=null){
                            oldPage.setAlpha(0);
                            oldPage.setVisibility(View.INVISIBLE);
                        }
                        if(newPage!=null){
                            newPage.setAlpha(1);
                        }
                        transitionBusy=false;
                    }
                });
            }
        }.start();
    }

    private void setTestServer() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Speed Test Server:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        if (SERVER_ADDR.isEmpty()) {
            input.setText("http://");
        } else {
            input.setText(SERVER_ADDR);
        }
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SERVER_ADDR = input.getText().toString();
                SharedPreferences.Editor edit = prefs.edit();
                edit.putString(SERVER_PREF_KEY, SERVER_ADDR);
                edit.commit();
                page_init();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void saveTestResult() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String currentDateandTime = sdf.format(new Date());
        String summary = currentDateandTime + "\n";
        summary += "\t\tserver: " + st.getSelectedServer().getServerIp() + "\n";
        summary += "\t\tping:   " + ((TextView)findViewById(R.id.pingText)).getText().toString() + " ms\n";
        summary += "\t\tjitter:  " + ((TextView)findViewById(R.id.jitterText)).getText().toString() + " ms\n";
        summary += "\t\tdown: " + ((TextView)findViewById(R.id.dlText)).getText().toString() + " Mbps\n";
        summary += "\t\tup:    " + ((TextView)findViewById(R.id.ulText)).getText().toString() + " Mbps\n";
        String history = prefs.getString(HIRTORY_PREF_KEY, "");
        if (history.isEmpty()) {
            history = summary;
        } else {
            history += "," + summary;
        }

        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(HIRTORY_PREF_KEY, history);
        edit.commit();
    }

    private void showHistoryResult() {
        String history = "";
        String[] results = prefs.getString(HIRTORY_PREF_KEY, "").split(",");
        for (String result: results) {
            history += result + "\n";
        }

        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        //alertDialog.setTitle("Result History");
        alertDialog.setMessage(history);

        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }
}
