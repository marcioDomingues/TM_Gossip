package gossip;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by MDo on 17/12/16.
 */
public class File_info implements Serializable {

    private static final long serialVersionUID = 8387950590016941525L;

    //REVIEW: use timestamp
    int version;

    int number_of_blocks;

    ArrayList<String> file_sections = new ArrayList<String>();


    public File_info( ArrayList<String> f ) {

        this.version= Integer.parseInt(f.get(0));
        this.number_of_blocks= Integer.parseInt(f.get(1));

        String[] n_section = f.get(2).split(" ");
        for(String s : n_section ){
            this.file_sections.add(s);
        }
    }


    public String toString(){
        return "version=" + version + ", #blocks=" + number_of_blocks + ", chunkList=" + file_sections ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        File_info file_info = (File_info) o;

        if (version != file_info.version) return false;
        if (number_of_blocks != file_info.number_of_blocks) return false;
        return file_sections != null ? file_sections.equals(file_info.file_sections) : file_info.file_sections == null;

    }

    @Override
    public int hashCode() {
        int result = version;
        result = 31 * result + number_of_blocks;
        result = 31 * result + (file_sections != null ? file_sections.hashCode() : 0);
        return result;
    }

    /////////////////////////////////////////////
    //TESTING
    public String printTeste(){
        return "File version: " + version + "\n#blocks: " + number_of_blocks + "\nlist of chunks " + file_sections ;
    }

    //TESTING
    public void getInfoFromFile( String folder ){
        File dir_ = new File( folder );
        File[] cont_ = dir_.listFiles();

        File dir_sub=cont_[cont_.length - 1];
        version = Integer.parseInt( dir_sub.getName() );

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
