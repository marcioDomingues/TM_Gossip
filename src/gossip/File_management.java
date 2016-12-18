package gossip;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by MDo on 17/12/16.
 */
public class File_management {

    String version;
    int number_of_blocks;

    ArrayList<String> file_sections = new ArrayList<String>();

    public File_management( ArrayList<String> f ) {

        this.version= f.get(0);
        this.number_of_blocks= Integer.parseInt(f.get(1));

        String[] n_section = f.get(2).split(" ");
        for(String s : n_section ){
            this.file_sections.add(s);
        }
    }


    public String toString(){
        return "to transfer >\nfile version: " + version + "\n#blocks: " + number_of_blocks + "\nlist of chunks " + file_sections ;
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
