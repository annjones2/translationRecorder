package wycliffeassociates.recordingapp.ProjectManager;

import android.app.Activity;

import android.app.DialogFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import wycliffeassociates.recordingapp.ConstantsDatabaseHelper;
import wycliffeassociates.recordingapp.ProjectManager.ActivityChapterList;
import wycliffeassociates.recordingapp.ProjectManager.Project;
import wycliffeassociates.recordingapp.ProjectManager.ProjectInfoDialog;
import wycliffeassociates.recordingapp.R;
import wycliffeassociates.recordingapp.Recording.RecordingScreen;

/**
 *
 * Creates a custom view for the audio entries in the file screen.
 *
 */
public class ProjectAdapter extends ArrayAdapter {
    //class for caching the views in a row
    private static class ViewHolder {
        TextView mLanguage, mBook;
        ImageButton mRecord, mInfo;
        LinearLayout mTextLayout;
    }

    LayoutInflater mLayoutInflater;
    List<Project> mProjectList;
    Activity mCtx;
    ConstantsDatabaseHelper mDb;

    public ProjectAdapter(Activity context, List<Project> projectList){
        super(context, R.layout.project_list_item, projectList);
        mCtx = context;
        mProjectList = projectList;
        mLayoutInflater = context.getLayoutInflater();
        mDb = new ConstantsDatabaseHelper(context);
    }

    public View getView(final int position, View convertView, final ViewGroup parent){
        ViewHolder holder;
        if(convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.project_list_item, null);
            holder = new ViewHolder();
            holder.mBook = (TextView) convertView.findViewById(R.id.book_text_view);
            holder.mLanguage = (TextView) convertView.findViewById(R.id.language_text_view);
            holder.mInfo = (ImageButton) convertView.findViewById(R.id.info_button);
            holder.mRecord = (ImageButton) convertView.findViewById(R.id.record_button);
            holder.mTextLayout = (LinearLayout) convertView.findViewById(R.id.text_layout);

            // Link the cached views to the convertView
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        String book = mDb.getBookName(mProjectList.get(position).getSlug());
        String language = mDb.getLanguageName(mProjectList.get(position).getTargetLanguage());

        holder.mBook.setText(book);
        holder.mLanguage.setText(language);

        holder.mRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Project.loadProjectIntoPreferences(mCtx, mProjectList.get(position));
                v.getContext().startActivity(new Intent(v.getContext(), RecordingScreen.class));
            }
        });

        holder.mInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment info = new ProjectInfoDialog();
                Bundle args = new Bundle();
                args.putParcelable(Project.PROJECT_EXTRA, mProjectList.get(position));
                info.setArguments(args);
                info.show(mCtx.getFragmentManager(), "title");
            }
        });

        holder.mTextLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), ActivityChapterList.class);
                intent.putExtra(Project.PROJECT_EXTRA, mProjectList.get(position));
                v.getContext().startActivity(intent);
            }
        });

        return convertView;
    }

}