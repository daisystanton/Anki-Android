package com.ichi2.anki;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Database layer for Ankidroid.
 * Supports the native Anki format through Android's SQLite driver.
 * 
 * @author Andrew Dubya, Nicolas Raoul
 */
public class AnkiDb {
	
	static public SQLiteDatabase database;
	
	/**
	 * Open a database connection to an ".anki" SQLite file.
	 */
	static public void openDatabase(String ankiFilename) throws SQLException {
		if (database != null) {
			database.close();
		}
        database = SQLiteDatabase.openDatabase(ankiFilename, null,
        	SQLiteDatabase.OPEN_READWRITE |
        	SQLiteDatabase.NO_LOCALIZED_COLLATORS);		
	}

	/**
	 * A card is Anki's question-answer entity.
	 * @see http://www.ichi2.net/anki/wiki/AddItems
	 */
	static public class Card {
		
		/**
		 * Identifier of the card, can be negative.
		 */
		public long id;
		
		/**
		 * Question and answer. Answer contains the reading.
		 */
		public String question, answer;
		
		/**
		 * Interval is right now used as a ranking in Ankidroid, which is quite
		 * different from Anki where it is used as an interval.
		 */
		public double interval;
		
		static final public String TABLE = "cards";
		static final public String COLUMNS = "";
		
		static public Card smallestIntervalCard() throws SQLException {
			Card card = oneFromCursor(
					AnkiDb.database.rawQuery(
							"SELECT id,interval,question,answer"
							+ " FROM cards"
							+ " WHERE priority > 0"
							+ " ORDER BY interval"
							+ " LIMIT 1"
						, null)
					);
			Log.i("anki", "Selected card id " + card.id + " with interval " + card.interval);
			return card;
		}
		
		/**
		 * Return one card starting from the given cursor.
		 */
		static private Card oneFromCursor(Cursor cursor) {
			if (cursor.isClosed()) {
				throw new SQLException();
			}
			cursor.moveToFirst();
			Card card = new Card();
			card.id = cursor.getLong(0);
			card.interval = cursor.getDouble(1);
			card.question = cursor.getString(2);
			card.answer = cursor.getString(3);
			return card;
		}
		
		/**
		 * Space this card because it has been successfully remembered.
		 */
		public void space() {
			double newInterval = 1;
			if (interval != 0)
				newInterval = 2*interval; // Very basic spaced repetition.
			ContentValues values = new ContentValues();
			values.put("interval", newInterval);
			AnkiDb.database.update("cards", values, "id='" + id + "'", null);
		}
		
		/**
		 * Reset this card because it has not been successfully remembered.
		 */
		public void reset() {
			ContentValues values = new ContentValues();
			values.put("interval", 0.1);
			AnkiDb.database.update("cards", values, "id='" + id + "'", null);
		}
	}
}