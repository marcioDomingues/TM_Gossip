package gossip;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by MDo on 17/12/16.
 */
public class File_management {


    String version = new String();
    int number_of_blocks;
    ArrayList<String> file_sections = new ArrayList<String>();

    public File_management( String folder ) {


    }

    static void teste (){

        File dir_sub=null;

        File dir_ = new File("data");
        File[] cont_ = dir_.listFiles();

        for ( File f : cont_){
            //System.out.println( f.getName() );
            version = f.getName();
            dir_sub = f;
        };


        if ( dir_sub!=null && dir_sub.isDirectory() ){
            File[] cont_sub = dir_sub.listFiles();
            int i=0;
            for ( File f : cont_sub){
                //System.out.println( f.getName() );
                file_sections.add(f.getName());
                i++;
            };
            number_of_blocks = i;
        }

        System.out.println( "to transfer > file version_ " + version + " n# blocks_ " + number_of_blocks + " list of f names " + file_sections.toString()  );

    }

    //File startupConfig = new File("config","startup_members");



}
