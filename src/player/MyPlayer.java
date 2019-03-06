package player;
/**
 * @author lie209
 * @date 2019/1/2 23:16
 */
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Scanner;


public class MyPlayer extends Application
{
    private File mediaFile=new File("init.mp4");   //初始播放文件
    private File historyFile=new File("history.txt");  //初始播放历史文件
    private BufferedWriter historyWriter;  //用于写入历史记录
    private MediaView mediaView;  //播放界面
    private Media media = new Media(mediaFile.toURI().toString());
    private MediaPlayer mediaPlayer=new MediaPlayer(media);   
    private MenuBar menuBar; 
    private Menu fileMenu,more;  //主菜单
    private MenuItem openFile,exit,streamMedia,vedioBack,aboutItem; //菜单选项
    private Slider volumeSlider;  //音量调节的滑动条
    private Text volume,rate;  
    private BorderPane mainView; //整体播放器的界面
    private Button playOrPause;  //播放和暂停按钮
    private Button stop;  //停止播放按钮
    private Text alreadyPlayed,gap,fullTime;  //显示已播放时间和总时间
    private ProgressBar progressBar;   //进度条
    private ComboBox<String> comboBoxRate;   //实现播放速率调节的组件
    private BorderPane bottomHBox;   
    private Stage vedioBackStage;  //视频区间回放功能的设置Stage
    private HBox playAndPause;   //存放播放和暂停按钮的Pane
    private HBox mediaPane,volumeAndSlider;    //播放界面和底部音量调节组件
    private HBox progressPane;   //放进度条和时间显示的Pane
    private VBox totalBottom;    //进度条以及以下部分全部放到这个Pane里
    private ArrayList<String> historyList=new ArrayList<>();  //存放播放历史记录的可变数组
    private ListView<String> historyListView;   //实现播放历史界面的ListView

    @Override
    public void start(Stage primaryStage)
    {
        try
        {
            //为写入历史纪录做准备
            historyWriter=new BufferedWriter(new FileWriter(historyFile,true));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        //各部分功能和界面的加入以及实现
        setMenu();  //菜单
        setMediaView();  //视频界面
        setProgressBar();  //进度条以及时间显示
        setBottomButtons();  //进度条底部两个控制按钮
        setVolumeAndRate();  //音量控制与播放速率控制
        setBottom();   //将底部几个模块合在一个Pane里以加入主要的Pane
        setMainView();  //主要界面的加入
        initMediaPlayer(mediaPlayer);   //初始化主界面的初始视频

        Scene scene=new Scene(mainView,1000,660);
        primaryStage.setTitle("简单播放器");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(ex->
        {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
    }

    /*加入整个底部*/
    public void setBottom()
    {
        //加入按钮和音量调节
        bottomHBox.setCenter(playAndPause);
        bottomHBox.setRight(volumeAndSlider);
        //MediaView底下的界面
        totalBottom=new VBox();
        totalBottom.getChildren().addAll(progressPane,bottomHBox);
    }


    /*加入MediaView*/
    public void setMediaView()
    {
        //播放界面
        mediaPane=new HBox();
        mediaPane.setAlignment(Pos.CENTER);
        mediaView=new MediaView(mediaPlayer);
        mediaPane.getChildren().add(mediaView);
        mediaPane.setStyle("-fx-background-color: black");  //市场上许多播放器都把背景设为黑色
        mediaView.setOnMouseClicked(ex->   //实现通用的一个功能，点击视频界面实现播放和暂停
        {
            if ("Play".equals(playOrPause.getText())) {
                playOrPause.setText("Pause");
                mediaPlayer.play();
            } else {
                playOrPause.setText("Play");
                mediaPlayer.pause();
            }
        });
    }

    /*加入音量条以及播放速率调节*/
    public void setVolumeAndRate()
    {
        //音量控制
        volume=new Text("Vol:");
        volumeSlider=new Slider();
        volumeSlider.setPrefWidth(50);
        volumeSlider.valueProperty().addListener(ov->
        {
            //一键静音功能的补充以及完善
            if(volumeSlider.getValue()!=0)
            {
                volume.setText("Vol:");
            }
            else
            {
                volume.setText("Mute");
            }
            //加入音量
            mediaPlayer.setVolume(volumeSlider.getValue()/100);
        });
        //一键静音
        volume.setOnMouseClicked(ov->
        {
            if("Vol:".equals(volume.getText()))
            {
                volume.setText("Mute");
                volumeSlider.setValue(0);
            }
            else
            {
                volume.setText("Vol:");
                volumeSlider.setValue(30);
            }
        });
        //音量以及倍速调整按钮
        volumeAndSlider=new HBox(5);
        volumeAndSlider.setPadding(new Insets(5,5,5,5));
        volumeAndSlider.setAlignment(Pos.CENTER_RIGHT);
        rate=new Text("Rate:");
        comboBoxRate=new ComboBox<>();
        String[] rateString={"0.5","0.75","1.0","1.5","2.0"};
        ObservableList<String> items= FXCollections.observableArrayList(rateString);
        comboBoxRate.setValue("1.0");
        comboBoxRate.getItems().addAll(items);
        comboBoxRate.setOnAction(ex->
                mediaPlayer.setRate(Double.valueOf(comboBoxRate.getValue())));


        volumeAndSlider.getChildren().addAll(volume,volumeSlider,rate,comboBoxRate);
    }

    /*加入进度条*/
    public void setProgressBar()
    {
        //中间的进度调节以及播放进度的显示
        progressPane=new HBox(5);
        progressPane.setPadding(new Insets(5,5,5,3));

        //时间显示
        alreadyPlayed=new Text("--:--:--");
        fullTime=new Text("--:--:--");
        gap=new Text("/");
        progressBar=new ProgressBar(0);
        progressPane.getChildren().addAll(progressBar,alreadyPlayed,gap,fullTime);
        progressBar.setOnMouseClicked(ev->
        {
            double x=ev.getX();
            double progressPercent=x/progressBar.getWidth();
            mediaPlayer.seek(mediaPlayer.getCycleDuration().multiply(progressPercent));
            alreadyPlayed.setText(getTimeFormated(mediaPlayer.getCycleDuration().multiply(progressPercent).toSeconds()));
            progressBar.setProgress(progressPercent);
        });
    }


    /*加入菜单 */
    public void setMenu()
    {
        //顶部菜单栏
        menuBar=new MenuBar();
        //File选项
        fileMenu=new Menu("文件");
        //打开文件
        openFile=new MenuItem("打开本地文件");
        FileChooser fileChooser=new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("MP4 Video", "*.mp4"),
                new FileChooser.ExtensionFilter("MP3 Music", "*.mp3"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        openFile.setOnAction(event -> {
            File file = fileChooser.showOpenDialog(new Stage());
            if (file != null) {
                //转换为URI格式
                mediaPlayer.dispose();  //释放前一个Media占用的资源
                media = new Media(file.toURI().toString());
                //写入历史纪录
                try{
                    historyWriter.write(file.getAbsolutePath()+"\r\n");
                    historyWriter.flush();  //将缓冲区的东西推进文件
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
                historyList.add(file.getAbsolutePath());  //将地址加入历史列表
                mediaPlayer = new MediaPlayer(media);  //用文件选择器选择的新的media构造一个新的MediaPlayer
                mediaView.setMediaPlayer(mediaPlayer); //将新的MediaPlayer放到播放界面里
                initMediaPlayer(mediaPlayer);  //初始化当前的MediaPlayer
            }
        });

        //正确退出程序
        exit=new MenuItem("退出");
        exit.setOnAction(ex->
        {
            Platform.exit();
            System.exit(0);
        });

        //流媒体播放
        streamMedia=new MenuItem("流媒体播放");
        streamMedia.setOnAction(event -> {
            //实现一个输入框，用于输入播放地址
            TextInputDialog inputURL = new TextInputDialog();
            inputURL.setTitle("流媒体播放");
            inputURL.setHeaderText("请输入播放地址:");
            inputURL.show();
            if (!"".equals(inputURL.getEditor().getText())) {  //输入地址有效则进行余下操作
                mediaPlayer.dispose();  //释放前一个Media所占用的资源
                media = new Media(inputURL.getEditor().getText());
                //将此次播放写入历史纪录
                try{
                    historyWriter.write(inputURL.getEditor().getText()+"\r\n");
                    historyWriter.flush();
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
                historyList.add(inputURL.getEditor().getText());
                //同打开文件时的视频初始化操作
                mediaPlayer = new MediaPlayer(media);
                mediaView.setMediaPlayer(mediaPlayer);
                initMediaPlayer(mediaPlayer);
            }

        });

        //历史记录功能的实现
        MenuItem history=new MenuItem("历史记录");
        //初始化文件列表
        try
        {
            Scanner historyReader=new Scanner(historyFile);
            while (historyReader.hasNext())
            {
                String filePath=historyReader.next();
                File mediaFileInList=new File(filePath);
                historyList.add(mediaFileInList.getAbsolutePath());  //用可变数组存放不同数量的文件地址
            }
        }catch (Exception ex)
        {
            ex.printStackTrace();
        }
        history.setOnAction(historyEvent->
        {
            //弹出一个让用户操作历史记录的界面
            VBox historyPane=new VBox(3);
            historyPane.setPadding(new Insets(5,5,5,5));
            HBox bottomButtons=new HBox(5);
            bottomButtons.setPadding(new Insets(5,5,5,5));
            Button playSelected=new Button("播放");
            Button clearHistory=new Button("清空记录");
            bottomButtons.getChildren().addAll(playSelected,clearHistory);
            //将文件列表写入ListView
            historyListView=new ListView<>(FXCollections.observableArrayList(historyList));
            historyListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            ScrollPane historyPaneLeft=new ScrollPane(historyListView);  //将ListView放到一个ScrollPane里
            //整体界面
            historyPane.getChildren().addAll(historyPaneLeft,bottomButtons);
            Scene historyScene=new Scene(historyPane);
            Stage historyStage=new Stage();
            historyStage.setTitle("播放历史");
            historyStage.setScene(historyScene);
            historyStage.show();
            //设定播放按钮的功能
            playSelected.setOnAction(ex->
            {
                File fileSelected=new File(historyListView.getSelectionModel().getSelectedItem());
                mediaPlayer.dispose();  //释放前一个Media占用的资源
                media = new Media(fileSelected.toURI().toString());
                //将此次播放写入历史纪录
                try{
                    historyWriter.write(fileSelected.getAbsolutePath()+"\r\n");
                    historyWriter.flush();
                }
                catch (Exception ev)
                {
                    ev.printStackTrace();
                }
                historyList.add(fileSelected.getAbsolutePath());
                 //同打开文件时的视频初始化操作
                mediaPlayer = new MediaPlayer(media); 
                mediaView.setMediaPlayer(mediaPlayer);
                initMediaPlayer(mediaPlayer);
                historyStage.close();
            });
            //设定清空历史按钮的功能
            clearHistory.setOnAction(clear->
            {
                try
                {
                    //将存放历史记录的文件清空
                    FileWriter clearWriter=new FileWriter(historyFile);
                    clearWriter.write("");
                    clearWriter.flush();
                    clearWriter.close();
                    historyStage.close();
                    historyList.clear();  //清空存放已播放文件地址的可变数组
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            });
        });
        fileMenu.getItems().addAll(openFile,streamMedia,history,exit);  //将“文件”菜单下的各子项加入“文件菜单”

        //More选项
        more=new Menu("更多");
        aboutItem=new MenuItem("关于");
        Alert introduction=new Alert(Alert.AlertType.INFORMATION,"作者：李德鑫");
        ImageView introductionImageView=new ImageView(new Image("1.png"));
        introductionImageView.setFitHeight(50);
        introductionImageView.setFitWidth(200);
        introduction.setGraphic(introductionImageView);
        introduction.setTitle("关于");
        introduction.setHeaderText("简单播放器 0.0.1");
        aboutItem.setOnAction(event -> introduction.show());
        menuBar.getMenus().addAll(fileMenu,more);

        //视频回看功能的实现
        vedioBack=new MenuItem("区间播放(experimental)");
        vedioBack.setOnAction(ex->
        {
            GridPane vedioBackPane =getGridPane();   //实现这个功能的操作在另一个方法实现。
            Scene vedioBackScene=new Scene(vedioBackPane,340,75);
            vedioBackStage=new Stage();
            vedioBackStage.setOnCloseRequest(ec->    //关闭视频回看功能时恢复截取片段前的视频状态
            {
                mediaPlayer.setStartTime(Duration.ZERO);
                mediaPlayer.setStopTime(mediaPlayer.getMedia().getDuration());
                playOrPause.setText("Play");
                mediaPlayer.seek(Duration.ZERO);
                mediaPlayer.pause();  //因为在STOPPED状态时MediaPalyer的seek()方法会不可用
                fullTime.setText(getTimeFormated(mediaPlayer.getMedia().getDuration().toSeconds()));  //恢复此前时间显示
                vedioBackStage.close();
            });
            vedioBackStage.setTitle("区间播放(experimental)");   //使用这个功能时，进度条会不可用
            vedioBackStage.setScene(vedioBackScene);
            vedioBackStage.show();
        });



        //声道调节功能的实现
        MenuItem soundTrack=new MenuItem("声道调节");
        soundTrack.setOnAction(ex->
        {
            Stage soundTrackStage=new Stage();   //操作界面
            soundTrackStage.setOnCloseRequest(event->
            {
                soundTrackStage.close();
            });
            Slider soundTrackSlider=new Slider();  //便于调节左右声道
            //设置为左右声道的最大和最小值
            soundTrackSlider.setMin(-1); 
            soundTrackSlider.setMax(1);
            soundTrackSlider.valueProperty().addListener(ev->
            {
                mediaPlayer.setBalance(soundTrackSlider.getValue());
            });
            Text left=new Text("Left");
            Text right=new Text("Right");
            Button defaultButton=new Button("恢复默认");  //方便一键恢复为左右平衡状态
            defaultButton.setOnAction(defaultEvent->
            {
                mediaPlayer.setBalance(0);
                soundTrackSlider.setValue(0);
            });
            HBox soundTrackPane=new HBox(5);
            soundTrackPane.setPadding(new Insets(10,10,10,10));
            soundTrackPane.getChildren().addAll(left,soundTrackSlider,right,defaultButton);
            Scene soundTrackScene=new Scene(soundTrackPane);
            soundTrackStage.setTitle("声道调节");
            soundTrackStage.setScene(soundTrackScene);
            soundTrackStage.show();
        });
        more.getItems().addAll(soundTrack,vedioBack,aboutItem);
    }



    /*将秒数转换成xx:xx:xx形式*/
    public String getTimeFormated(double seconds)
    {
        int hours=(int)seconds/3600;
        int minutes=(int)(seconds/60-hours*60);
        int second=(int)(seconds-hours*3600-minutes*60);

        return numbersTransform(hours)+":"+numbersTransform(minutes)+":"+numbersTransform(second);
    }

    /*初始化当前mediaPlayer*/
    public void initMediaPlayer(MediaPlayer mediaPlayer)
    {
        mediaPlayer.setOnEndOfMedia(()->
        {
            mediaPlayer.seek(mediaPlayer.getStartTime());
            progressBar.setProgress(0);
            alreadyPlayed.setText(getTimeFormated(mediaPlayer.getStartTime().toSeconds()));
            //播放器状态为STOPPED时，seek将不起作用，此时需将播放器状态转换为PAUSED
            playOrPause.setText("Play");
            mediaPlayer.pause();
        });
        //视频准备完毕时获取结束时间
        mediaPlayer.setOnReady(()->
        {
            mediaPlayer.setRate(1.0);
            comboBoxRate.setValue("1.0");
            progressBar.setProgress(0);
            volumeSlider.setValue(30);
            playOrPause.setText("Play");
            alreadyPlayed.setText("00:00:00");
            fullTime.setText(getTimeFormated(mediaPlayer.getStopTime().toSeconds()));
        });
        //视频播放时currentTimeProperty会改变，添加一个监听器使其同时改变已播放时间
        mediaPlayer.currentTimeProperty().addListener(ov->
        {
            alreadyPlayed.setText(getTimeFormated(mediaPlayer.getCurrentTime().toSeconds()));
            progressBar.setProgress((mediaPlayer.getCurrentTime().toMillis()-mediaPlayer.getStartTime().toMillis())/mediaPlayer.getCycleDuration().toMillis());
        });
    }

    /*转换数字为两位的*/
    public String numbersTransform(int number)
    {
        if(number<10)
        {
            return "0"+number;
        }
        else
        {
            return ""+number;
        }
    }

    /*实现视频回看功能的界面*/
    public GridPane getGridPane()
    {
        //实现操作界面
        GridPane vedioBackPane = new GridPane();
        vedioBackPane.setPadding(new Insets(5,10,5,10));
        TextField hours1=new TextField();
        TextField minutes1=new TextField();
        TextField seconds1=new TextField();
        TextField hours2=new TextField();
        TextField minutes2=new TextField();
        TextField seconds2=new TextField();
        vedioBackPane.add(new Text("开始时间:"), 0, 0);
        vedioBackPane.add(hours1, 1, 0);
        vedioBackPane.add(new Text(":"), 2, 0);
        vedioBackPane.add(minutes1, 3, 0);
        vedioBackPane.add(new Text(":"), 4, 0);
        vedioBackPane.add(seconds1, 5, 0);
        vedioBackPane.add(new Text("结束时间:"), 0, 1);
        vedioBackPane.add(hours2, 1, 1);
        vedioBackPane.add(new Text(":"), 2, 1);
        vedioBackPane.add(minutes2, 3, 1);
        vedioBackPane.add(new Text(":"), 4, 1);
        vedioBackPane.add(seconds2, 5, 1);
        Button done=new Button("完成");
        Button exit=new Button("退出");
        vedioBackPane.add(done,3,2);
        vedioBackPane.add(exit,5,2);
        done.setOnAction(ex->  //确定按钮的功能实现
        {
            //加入起始和结束时间
            double startMillis=Double.parseDouble(hours1.getText())*3600000+Double.parseDouble(minutes1.getText())*60000+Double.parseDouble(seconds1.getText())*1000;
            double stopMillis=Double.parseDouble(hours2.getText())*3600000+Double.parseDouble(minutes2.getText())*60000+Double.parseDouble(seconds2.getText())*1000;
            mediaPlayer.setStartTime(new Duration(startMillis));
            mediaPlayer.setStopTime(new Duration(stopMillis));
            mediaPlayer.seek(mediaPlayer.getStartTime());
            progressBar.setProgress(0);  //使进度条进度为零
            //显示设置的开始时间和结束时间
            alreadyPlayed.setText(getTimeFormated(mediaPlayer.getStartTime().toSeconds()));
            fullTime.setText(getTimeFormated(new Duration(stopMillis).toSeconds()));
        });
        exit.setOnAction(ex->  //恢复截取片段前的视频状态
        {
            mediaPlayer.setStartTime(Duration.ZERO);
            mediaPlayer.setStopTime(mediaPlayer.getMedia().getDuration());
            playOrPause.setText("Play");
            mediaPlayer.seek(Duration.ZERO);
            mediaPlayer.pause();
            fullTime.setText(getTimeFormated(mediaPlayer.getMedia().getDuration().toSeconds()));
            vedioBackStage.close();
        });
        return vedioBackPane;
    }



    /*加入主要界面框架*/
    public void setMainView()
    {
        //整体界面框架
        mainView=new BorderPane();
        mainView.setTop(menuBar);
        mainView.setCenter(mediaPane);
        mainView.setBottom(totalBottom);
        //界面适应
        volumeSlider.setPrefWidth(mainView.getWidth()/7);
        mainView.widthProperty().addListener(ov->
        {
            mediaView.setFitWidth(mainView.getWidth());
            progressBar.setPrefWidth(mainView.getWidth());
            volumeSlider.setPrefWidth(mainView.getWidth()/7);
        });
        mainView.heightProperty().addListener(ov->
                mediaView.setFitHeight(mainView.getHeight()-90));

    }

    /*加入底部Play与Stop按钮的功能*/
    public void setBottomButtons()
    {
        //播放与暂停键
        playOrPause=new Button("Play");
        playOrPause.setPrefSize(75,25);
        playOrPause.setOnAction(event ->
        {
            if ("Play".equals(playOrPause.getText())) {
                playOrPause.setText("Pause");
                mediaPlayer.play();
            } else {
                playOrPause.setText("Play");
                mediaPlayer.pause();
            }

        });
        //停止播放键
        stop=new Button("Stop");
        stop.setPrefSize(75,25);
        stop.setOnAction(event -> {
            mediaPlayer.seek(mediaPlayer.getStartTime());
            progressBar.setProgress(0);
            //防止seek不起作用的bug发生
            playOrPause.setText("Play");
            mediaPlayer.pause();
        });

        //最底下那一栏
        bottomHBox=new BorderPane();
        //播放与暂停按钮
        playAndPause=new HBox(15);
        playAndPause.setAlignment(Pos.CENTER);
        playAndPause.setPadding(new Insets(0,5,5,5));
        playAndPause.getChildren().addAll(playOrPause,stop);
    }



    public static void main(String[] args)
    {
        launch(args);
    }

}




/*需求
*倍速播放      【已实现】
* 一键静音  【已实现】
* 播放流媒体  【已实现但未验证】
* 播放本地文件  【已实现】
* 视频自适应窗口  【已实现】
* 软件UI美化   【等我学了前端再来实现】
* 根据文件类型MP3或MP4变换播放模式   【等我功力更加深厚再来实现】
* 显示已播放时间和总播放时间  【已实现】
* 在ProgressBar上轻松改变视频播放进度  【已实现】
* 视频在选定区间回放    【已实现】
* 重写 About 界面    【部分实现】
* 左右声道调节      【已实现】
* 鼠标点击视频界面实现暂停和播放  【已实现】
* 软件多语言切换  【取消，实用性不大】
*/




