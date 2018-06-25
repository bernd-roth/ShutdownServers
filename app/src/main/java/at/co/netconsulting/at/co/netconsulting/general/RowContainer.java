package at.co.netconsulting.at.co.netconsulting.general;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class RowContainer {
    private ArrayList<Integer> row = new ArrayList<>();

    public RowContainer()
    {
    }

    public ArrayList<Integer> getRow() {
        return row;
    }

    public void setRow(ArrayList<Integer> row) {
        this.row = row;
    }

    public void addRow(int addedRow)
    {
        this.row.add(addedRow);
    }
}
