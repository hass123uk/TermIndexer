package DomainModels;

import java.util.Date;

public class Document {

    private int document_ID;
    private String document_Path;
    private Date document_IndexTime;

    public Document(int ID, String path, Date indexTime) {
        this.document_ID = ID;
        this.document_Path = path;
        this.document_IndexTime = indexTime;
    }

    public int getDocument_ID() {
        return document_ID;
    }

    public void setDocument_ID(int document_ID) {
        this.document_ID = document_ID;
    }

    public String getDocument_Path() {
        return document_Path;
    }

    public void setDocument_Path(String document_Path) {
        this.document_Path = document_Path;
    }

    public Date getDocument_IndexTime() {
        return document_IndexTime;
    }

    public void setDocument_IndexTime(Date document_IndexTime) {
        this.document_IndexTime = document_IndexTime;
    }
}
