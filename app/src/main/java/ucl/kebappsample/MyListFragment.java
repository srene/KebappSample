package ucl.kebappsample;


import android.app.ListFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

public class MyListFragment extends ListFragment implements OnItemClickListener {
    ArrayList<String> listItems=new ArrayList<String>();
    ArrayAdapter<String> adapter;
    int clickCounter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.list_fragment, container, false);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //ArrayAdapter adapter = ArrayAdapter.createFromResource(getActivity(), listItems, android.R.layout.simple_list_item_1);
        adapter=new ArrayAdapter<String>(getActivity(),android.R.layout.simple_list_item_1, listItems);
        setListAdapter(adapter);
        getListView().setOnItemClickListener(this);
      //  addItems();
    }

    //public void addItems() {
    //    adapter.add("Clicked : "+clickCounter++);
    //}
    public void addItems(String str) {
        adapter.add("User : "+str);
    }

    public void clearItems()
    {
        adapter.clear();
    }
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,long id) {
        //Toast.makeText(getActivity(), "Item: " + position, Toast.LENGTH_SHORT).show();
        //addItems();
    }
}