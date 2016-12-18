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
        getInfoFromFile( folder );
    }




    public void print_test(){
        System.out.println( "to transfer > file version_ " + version + " n# blocks_ " + number_of_blocks + " list of f names " + file_sections.toString()  );
    }

    public void getInfoFromFile( String folder ){
        File dir_ = new File( folder );
        File[] cont_ = dir_.listFiles();

        File dir_sub=cont_[cont_.length - 1];
        version = dir_sub.getName();

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
    }

    //File startupConfig = new File("config","startup_members");



}
