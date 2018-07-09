package jp.ac.titech.itpro.sdl.simplemap;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;


class InternalFileReadWrite {

    private Context context;
    private String LOGFILE_NAME = "log.txt";
    private String MEMFILE_NAME = "memory.txt";
    private StringBuffer stringBuffer;

    InternalFileReadWrite(Context context){
        this.context = context;
    }

    void clearFile(){
        Log.d("debug","clearFile");
        // ファイル削除
        context.deleteFile(LOGFILE_NAME);
        context.deleteFile(MEMFILE_NAME);
        // StringBuffer clear
//        stringBuffer.setLength(0);
    }

    void writeLogFile (String str) {
        writeFile(str, LOGFILE_NAME);
    }

    void writeLMemFile (String str) {
        writeFile(str, MEMFILE_NAME);
    }

    // ファイルを保存
    void writeFile(String str, String filename) {
        Log.d("debug","writeFile");

        // try-with-resources
        try (FileOutputStream fileOutputstream =
                     context.openFileOutput(filename,
                             Context.MODE_APPEND)){

            fileOutputstream.write(str.getBytes());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String readLogFile () {
        return readFile(LOGFILE_NAME);
    }

    String readMemFile () {
        return readFile(MEMFILE_NAME);
    }

    // ファイルを読み出し
    String readFile(String filename) {
        Log.d("debug","readFile");

        stringBuffer = new StringBuffer();

        // try-with-resources
        try (FileInputStream fileInputStream = context.openFileInput(filename);
             BufferedReader reader= new BufferedReader(
                     new InputStreamReader(fileInputStream,"UTF-8"))
        ) {

            String lineBuffer;

            while( (lineBuffer = reader.readLine()) != null ) {
                stringBuffer.append(lineBuffer);
                stringBuffer.append(System.getProperty("line.separator"));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return stringBuffer.toString();
    }
}