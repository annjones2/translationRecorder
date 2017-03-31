package org.wycliffeassociates.translationrecorder.project;

/**
 * Created by Joe on 3/31/2017.
 */

public class ProjectSlugs {

    String mLanguage;
    String mAnthology;
    String mVersion;
    int mBookNumber;
    String mBook;
    int mChapter;
    int mStartVerse;
    int mEndVerse;

    int mTake;

    public ProjectSlugs(String language, String anthology, String version, int bookNumber, String book,
                        int chapter, int startVerse, int endVerse, int take)
    {
        mLanguage = language;
        mAnthology = anthology;
        mVersion = version;
        mBookNumber = bookNumber;
        mBook = book;
        mChapter = chapter;
        mStartVerse = startVerse;
        mEndVerse = endVerse;
        mTake = take;
    }

    public String getLanguage() {
        return mLanguage;
    }

    public String getAnthology() {
        return mAnthology;
    }

    public String getVersion() {
        return mVersion;
    }

    public int getBookNumber() {
        return mBookNumber;
    }

    public String getBook() {
        return mBook;
    }

    public int getChapter() {
        return mChapter;
    }

    public int getStartVerse() {
        return mStartVerse;
    }

    public int getTake() {
        return mTake;
    }

    public int getEndVerse() {
        //if there is no end verse, there is no verse range, so the end verse is the start verse
        if(mEndVerse == -1) {
            return mStartVerse;
        }
        return mEndVerse;
    }

}