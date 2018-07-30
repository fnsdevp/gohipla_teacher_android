package com.fnspl.hiplaedu_teacher.adapter;

/**
 * Created by User on 8/3/2017.
 */

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.fnspl.hiplaedu_teacher.R;
import com.fnspl.hiplaedu_teacher.model.StudentOutOfClassData;
import com.fnspl.hiplaedu_teacher.utils.CONST;
import com.fnspl.hiplaedu_teacher.utils.NetworkUtility;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;


public class StudentOutOfClassAdapter extends RecyclerView.Adapter<StudentOutOfClassAdapter.ViewHolder> {

    private List<StudentOutOfClassData> values;
    private Context context ;
    private OnMarkAbsentClickListener mListener;

    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView tv_student_name, tv_in_time, tv_out_time;
        public View layout;
        public Button btn_mark_absent;
        public ImageView img_profile_pic;

        public ViewHolder(View v) {
            super(v);
            layout = v;

            tv_student_name = (TextView) v.findViewById(R.id.tv_student_name);
            tv_in_time = (TextView) v.findViewById(R.id.tv_in_time);
            tv_out_time = (TextView) v.findViewById(R.id.tv_out_time);
            btn_mark_absent = (Button) v.findViewById(R.id.btn_mark_absent);
            img_profile_pic = (ImageView) v.findViewById(R.id.img_profile_pic);
        }
    }

    public void notifyDataChange(List<StudentOutOfClassData> data) {
        this.values = data ;
        notifyDataSetChanged();
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public StudentOutOfClassAdapter(Context context, List<StudentOutOfClassData> myDataset) {
        this.context = context ;
        values = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public StudentOutOfClassAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                                  int viewType) {
        // create a new view
        LayoutInflater inflater = LayoutInflater.from(
                parent.getContext());
        View v =
                inflater.inflate(R.layout.item_student_out, parent, false);
        // set the view's size, margins, paddings and layout parameters
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element

        ImageLoader.getInstance().displayImage(NetworkUtility.IMAGE_BASEURL+""+values.get(position).getPhoto(),
                holder.img_profile_pic, CONST.ErrorWithLoaderNormalCorner);

        holder.tv_student_name.setText(""+values.get(position).getName());
        holder.tv_in_time.setText(""+values.get(position).getStudent_in_time());
        holder.tv_out_time.setText(""+values.get(position).getStudent_out_time());

        holder.btn_mark_absent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mListener!=null){
                    mListener.onMarkAbsen(position, values.get(position));
                }
            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return values.size();
    }

    public interface OnMarkAbsentClickListener{
        void onMarkAbsen(int position, StudentOutOfClassData data);
    }

    public void setOnAttendanceClickListener(OnMarkAbsentClickListener mListener){
        this.mListener = mListener;
    }
}