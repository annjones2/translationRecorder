package wycliffeassociates.recordingapp.widgets;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.lang.ref.SoftReference;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import wycliffeassociates.recordingapp.FilesPage.FileNameExtractor;
import wycliffeassociates.recordingapp.Playback.PlaybackScreen;
import wycliffeassociates.recordingapp.ProjectManager.CheckingDialog;
import wycliffeassociates.recordingapp.ProjectManager.Project;
import wycliffeassociates.recordingapp.ProjectManager.ProjectDatabaseHelper;
import wycliffeassociates.recordingapp.ProjectManager.RatingDialog;
import wycliffeassociates.recordingapp.ProjectManager.UnitCardAdapter;
import wycliffeassociates.recordingapp.R;
import wycliffeassociates.recordingapp.Recording.RecordingScreen;
import wycliffeassociates.recordingapp.Recording.WavFile;
import wycliffeassociates.recordingapp.Utils;

/**
 * Created by leongv on 7/28/2016.
 */
public class UnitCard {

    public interface OnClickListener extends View.OnClickListener {
        void onClick(View v, UnitCardAdapter.ViewHolder vh, List<Integer> expandedCards, int position);
    }

    // Constants
    //public static boolean RATING_MODE = true;
    //public static boolean CHECKING_MODE = false;

    // State
    private boolean mIsExpanded = false;
    private int mTakeIndex = 0;
    private boolean mIsEmpty = true;
    private int mCurrentTakeRating;

    // Attributes
    private String mTitle;
    private final Project mProject;
    private final int mChapter;
    private final int mFirstVerse;
    private final int mEndVerse;
    private SoftReference<List<File>> mTakeList;
    private SoftReference<AudioPlayer> mAudioPlayer;
    private Activity mCtx;
    private Resources.Theme mTheme;


    // Constructors
    public UnitCard(Activity ctx, Project project, int chapter, int firstVerse, int endVerse) {
        mTitle = Utils.capitalizeFirstLetter(project.getMode()) + " " + firstVerse;
        mFirstVerse = firstVerse;
        mEndVerse = endVerse;
        mChapter = chapter;
        mProject = project;
        mCtx = ctx;
        mTheme = mCtx.getTheme();
    }


    // Setters
    public void setTitle(String title) {
        mTitle = title;
    }


    // Getters
    public String getTitle() {
        return mTitle;
    }

    public int getStartVerse(){
        return mFirstVerse;
    }

    public boolean isExpanded() {
        return mIsExpanded;
    }

    public boolean isEmpty() {
        return mIsEmpty;
    }


    // Private Methods
    private AudioPlayer initializeAudioPlayer(UnitCardAdapter.ViewHolder vh) {
        AudioPlayer ap = new AudioPlayer(vh.mProgress, vh.mDuration, vh.mTakePlayPauseBtn, vh.mSeekBar);
        mAudioPlayer = new SoftReference<AudioPlayer>(ap);
        return ap;
    }

    private AudioPlayer getAudioPlayer(UnitCardAdapter.ViewHolder vh) {
        AudioPlayer ap = null;
        if (mAudioPlayer != null) {
            ap = mAudioPlayer.get();
        }
        if (ap == null) {
            ap = initializeAudioPlayer(vh);
        }
        return ap;
    }

    private void refreshAudioPlayer(UnitCardAdapter.ViewHolder vh) {
        AudioPlayer ap = getAudioPlayer(vh);
        if (!ap.isLoaded()) {
            ap.reset();
            List<File> takes = getTakeList();
            if (mTakeIndex < takes.size()) {
                ap.loadFile(getTakeList().get(mTakeIndex));
            }
        }
        ap.refreshView(vh.mProgress, vh.mDuration, vh.mTakePlayPauseBtn, vh.mSeekBar);
    }

    private List<File> getTakeList() {
        List<File> takes = null;
        if (mTakeList != null) {
            takes = mTakeList.get();
        }
        if (takes == null) {
            takes = populateTakeList();
        }
        return takes;
    }

    private List<File> populateTakeList() {
        File root = Project.getProjectDirectory(mProject);
        String chap = FileNameExtractor.chapterIntToString(mProject, mChapter);
        File folder = new File(root, chap);
        File[] files = folder.listFiles();
        FileNameExtractor fne;
        int first = mFirstVerse;
        int end = mEndVerse;
        if (mProject.getMode().equals("verse") || first == end) {
            end = -1;
        }
        //Get only the files of the appropriate unit
        List<File> resultFiles = new ArrayList<>();
        if (files != null) {
            for (File file : files) {
                fne = new FileNameExtractor(file);
                if (fne.getStartVerse() == first && fne.getEndVerse() == end) {
                    resultFiles.add(file);
                }
            }
        }
        Collections.sort(resultFiles, new Comparator<File>() {
            @Override
            public int compare(File f, File s) {
                Long first = f.lastModified();
                Long second = s.lastModified();
                return first.compareTo(second);
            }
        });
        mTakeList = new SoftReference<>(resultFiles);
        return resultFiles;
    }

    private void refreshTakes(UnitCardAdapter.ViewHolder vh) {
        //if the soft reference still has the takes, cool, if not, repopulate them
        List<File> takes = getTakeList();
        refreshTakeText(takes, vh.mCurrentTake, vh.mCurrentTakeTimeStamp);
        if(takes.size() > 0) {
            File take = takes.get(mTakeIndex);
            refreshTakeRating(take, vh.mTakeRatingBtn);
            refreshSelectedTake(take, vh.mTakeSelectBtn);
        }
    }

    private void refreshSelectedTake(File take, ImageButton selectTake){
        ProjectDatabaseHelper db = new ProjectDatabaseHelper(mCtx);
        FileNameExtractor fne = new FileNameExtractor(take);
        int chosen = db.getChosenTake(fne);
        if(chosen == fne.getTake()){
            selectTake.setActivated(true);
        } else {
            selectTake.setActivated(false);
        }
    }

    private void refreshTakeRating(File take, FourStepImageView ratingView){
        ProjectDatabaseHelper db = new ProjectDatabaseHelper(mCtx);
        FileNameExtractor fne = new FileNameExtractor(take);
        mCurrentTakeRating = db.getTakeRating(fne);
        ratingView.setStep(mCurrentTakeRating);
        ratingView.invalidate();
        db.close();
    }

    private void refreshTakeText(List<File> takes, final TextView takeView, final TextView timestamp) {
        final String text;
        if (takes.size() > 0) {
            text = "Take " + (mTakeIndex + 1) + " of " + takes.size();
            long created = takes.get(mTakeIndex).lastModified();
            timestamp.setText(convertTime(created));
        } else {
            text = "Take 0 of " + takes.size();
            timestamp.setText("");
        }
        takeView.setText(text);
        takeView.invalidate();
    }

    private String convertTime(long time){
        Date date = new Date(time);
        Format format = new SimpleDateFormat("MMMM d, yyyy  HH:mm ");
        return format.format(date);
    }


    // Public API
    public void refreshUnitStarted(Project project, int chapter, int startVerse) {
        File dir = Project.getProjectDirectory(project);
        String chapterString = FileNameExtractor.chapterIntToString(project, chapter);
        File chapterDir = new File(dir, chapterString);
        if(chapterDir.exists()) {
            File[] files = chapterDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    FileNameExtractor fne = new FileNameExtractor(f);
                    if (fne.getStartVerse() == startVerse) {
                        mIsEmpty = false;
                        return;
                    }
                }
            }
        }
        mIsEmpty = true;
    }

    public void expand(UnitCardAdapter.ViewHolder vh) {
        refreshTakes(vh);
        refreshAudioPlayer(vh);
        vh.mCardBody.setVisibility(View.VISIBLE);
        vh.mCardFooter.setVisibility(View.VISIBLE);
        vh.mUnitActions.setActivated(true);
        mIsExpanded = true;
    }

    public void collapse(UnitCardAdapter.ViewHolder vh) {
        vh.mCardBody.setVisibility(View.GONE);
        vh.mCardFooter.setVisibility(View.GONE);
        vh.mUnitActions.setActivated(false);
        mIsExpanded = false;
    }

    public void raise(UnitCardAdapter.ViewHolder vh) {
        vh.mCardView.setCardElevation(8f);
        vh.mCardContainer.setBackgroundColor(mCtx.getResources().getColor(R.color.accent));
        vh.mUnitTitle.setTextColor(mCtx.getResources().getColor(R.color.text_light));
        vh.mUnitActions.setEnabled(false);
    }

    public void drop(UnitCardAdapter.ViewHolder vh) {
        vh.mCardView.setCardElevation(2f);
        vh.mCardContainer.setBackgroundColor(mCtx.getResources().getColor(R.color.card_bg));
        vh.mUnitTitle.setTextColor(
                mCtx.getResources().getColor((isEmpty())? R.color.primary_text_disabled_material_light : R.color.primary_text_default_material_light)
        );
        vh.mUnitActions.setEnabled(true);
    }

    public void playAudio(UnitCardAdapter.ViewHolder vh) {
        getAudioPlayer(vh).play();
    }

    public void pauseAudio(UnitCardAdapter.ViewHolder vh) {
        getAudioPlayer(vh).pause();
    }

    public void destroyAudioPlayer() {
        if (mAudioPlayer != null) {
            AudioPlayer ap = mAudioPlayer.get();
            ap.cleanup();
            mAudioPlayer = null;
        }
    }

    public View.OnClickListener getUnitRecordOnClick(final UnitCardAdapter.ViewHolder vh) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pauseAudio(vh);
                Project.loadProjectIntoPreferences(mCtx, mProject);
                view.getContext().startActivity(RecordingScreen.getNewRecordingIntent(mCtx, mProject, mChapter, mFirstVerse));
            }
        };
    }

    public View.OnClickListener getUnitExpandOnClick(final UnitCardAdapter.ViewHolder vh, final List<Integer> expandedCards) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = vh.getAdapterPosition();
                if (!isExpanded()) {
                    expand(vh);
                    if (!expandedCards.contains(position)) {
                        expandedCards.add(position);
                    }
                } else {
                    pauseAudio(vh);
                    collapse(vh);
                    if (expandedCards.contains(position)) {
                        expandedCards.remove(expandedCards.indexOf(position));
                    }
                }
            }
        };
    }

    public View.OnClickListener getTakeIncrementOnClick(final UnitCardAdapter.ViewHolder vh){
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<File> takes = getTakeList();
                if(takes.size() > 0) {
                    mTakeIndex++;
                    if (mTakeIndex >= takes.size()) {
                        mTakeIndex = 0;
                    }
                    destroyAudioPlayer();
                    refreshTakes(vh);
                    refreshAudioPlayer(vh);
                }
            }
        };
    }

    public View.OnClickListener getTakeDecrementOnClick(final UnitCardAdapter.ViewHolder vh){
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<File> takes = getTakeList();
                if(takes.size() > 0) {
                    mTakeIndex--;
                    if (mTakeIndex < 0) {
                        mTakeIndex = takes.size() - 1;
                    }
                    destroyAudioPlayer();
                    refreshTakes(vh);
                    refreshAudioPlayer(vh);
                }
            }
        };
    }

    public View.OnClickListener getTakeDeleteOnClick(final UnitCardAdapter.ViewHolder vh, final UnitCardAdapter adapter){
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseAudio(vh);
                final List<File> takes = getTakeList();
                if (takes.size() > 0) {
                    AlertDialog dialog = new AlertDialog.Builder(mCtx)
                        .setTitle("Delete take?")
                        .setIcon(R.drawable.ic_delete_black_36dp)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                File selectedFile = takes.get(mTakeIndex);
                                FileNameExtractor fne = new FileNameExtractor(selectedFile);
                                ProjectDatabaseHelper db = new ProjectDatabaseHelper(mCtx);
                                db.deleteTake(fne);
                                db.close();
                                takes.get(mTakeIndex).delete();
                                takes.remove(mTakeIndex);
                                //keep the same index in the list, unless the one removed was the last take.
                                if (mTakeIndex > takes.size() - 1) {
                                    mTakeIndex--;
                                    //make sure the index is not negative
                                    mTakeIndex = Math.max(mTakeIndex, 0);
                                }
                                refreshTakes(vh);
                                if (takes.size() > 0) {
                                    AudioPlayer ap = getAudioPlayer(vh);
                                    ap.reset();
                                    ap.loadFile(takes.get(mTakeIndex));
                                } else {
                                    mIsEmpty = true;
                                    collapse(vh);
                                    destroyAudioPlayer();
                                    adapter.notifyItemChanged(vh.getAdapterPosition());
                                }
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .create();
                    dialog.show();
                }
            }
        };
    }

    public View.OnClickListener getTakeEditOnClickListener(final UnitCardAdapter.ViewHolder vh){
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<File> takes = getTakeList();
                if(takes.size() > 0) {
                    pauseAudio(vh);
                    WavFile wavFile = new WavFile(takes.get(mTakeIndex));
                    Intent intent = PlaybackScreen.getPlaybackIntent(v.getContext(), wavFile, mProject, mChapter, mFirstVerse);
                    v.getContext().startActivity(intent);
                }
            }
        };
    }

    public View.OnClickListener getTakePlayPauseOnClick(final UnitCardAdapter.ViewHolder vh){
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(vh.mTakePlayPauseBtn.isActivated()) {
                    pauseAudio(vh);
                } else {
                    playAudio(vh);
                }
            }
        };
    }

    public View.OnClickListener getTakeRatingOnClick(final UnitCardAdapter.ViewHolder vh) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<File> takes = getTakeList();
                if(takes.size() > 0) {
                    pauseAudio(vh);
                    String name = takes.get(mTakeIndex).getName();
                    RatingDialog dialog = RatingDialog.newInstance(name, mCurrentTakeRating);
                    dialog.show(mCtx.getFragmentManager(), "single_take_rating");
                }
            }
        };
    }

    public View.OnClickListener getTakeSelectOnClick(final UnitCardAdapter.ViewHolder holder) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                List<File> takes = getTakeList();
                if(takes.size() > 0) {
                    ProjectDatabaseHelper db = new ProjectDatabaseHelper(mCtx);
                    FileNameExtractor fne = new FileNameExtractor(takes.get(mTakeIndex));
                    if(view.isActivated()){
                        view.setActivated(false);
                        db.removeSelectedTake(fne);
                    } else {
                        view.setActivated(true);
                        db.setSelectedTake(fne);
                    }
                }
            }
        };
    }

}
