/*
 * Tigase Jabber/XMPP Multi-User Chat Component
 * Copyright (C) 2008 "Bartosz M. Małkowski" <bartosz.malkowski@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.muc.history;

import tigase.component.PacketWriter;
import tigase.db.DataRepository;
import tigase.db.Repository;
import tigase.muc.Affiliation;
import tigase.muc.Room;
import tigase.muc.RoomConfig.Anonymity;
import tigase.server.Packet;
import tigase.util.TigaseStringprepException;
import tigase.xml.Element;
import tigase.xmpp.JID;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;

/**
 * @author bmalkow
 *
 */
public class DerbySqlHistoryProvider extends AbstractJDBCHistoryProvider {

	public static final String ADD_MESSAGE_QUERY_VAL = "insert into muc_history (room_name, event_type, timestamp, sender_jid, sender_nickname, body, public_event, msg) values (?, 1, ?, ?, ?, ?, ?, ?)";

	private static final String CREATE_MUC_HISTORY_TABLE_VAL = "create table muc_history (" + "room_name char(128) NOT NULL,\n"
			+ "event_type int, \n" + "timestamp bigint,\n" + "sender_jid varchar(2049),\n" + "sender_nickname char(128),\n"
			+ "body varchar(4096),\n " + "public_event BOOLEAN,\n " + "msg varchar(32672) " + ")";

	public static final String DELETE_MESSAGES_QUERY_VAL = "delete from muc_history where room_name=?";

	public static final String GET_MESSAGES_MAXSTANZAS_QUERY_VAL = "select room_name, event_type, timestamp, sender_jid, sender_nickname, body, msg from muc_history where room_name=? order by timestamp desc";

	public static final String GET_MESSAGES_SINCE_QUERY_VAL = "select room_name, event_type, timestamp, sender_jid, sender_nickname, body, msg from muc_history where room_name=? and timestamp >= ? order by timestamp desc";

	/**
	 */
	public DerbySqlHistoryProvider() {
	}

	/** {@inheritDoc} */
	@Override
	public void addJoinEvent(Room room, Date date, JID senderJID, String nickName) {
		// TODO Auto-generated method stub

	}

	/** {@inheritDoc} */
	@Override
	public void addLeaveEvent(Room room, Date date, JID senderJID, String nickName) {
		// TODO Auto-generated method stub

	}

	/** {@inheritDoc} */
	@Override
	public void addSubjectChange(Room room, Element message, String subject, JID senderJid, String senderNickname, Date time) {
		// TODO Auto-generated method stub

	}

	/** {@inheritDoc} */
	@Override
	public void getHistoryMessages(Room room, JID senderJID, Integer maxchars, Integer maxstanzas, Integer seconds, Date since,
			PacketWriter writer) {
		ResultSet rs = null;
		final String roomJID = room.getRoomJID().toString();
		try {
			Integer maxStanzas = null;
			if (since != null) {
				PreparedStatement st = dataRepository.getPreparedStatement(senderJID.getBareJID(),
						GET_MESSAGES_SINCE_QUERY_KEY);
				synchronized (st) {
					st.setString(1, roomJID);
					st.setLong(2, since.getTime());
					rs = st.executeQuery();
					processResultSet(room, senderJID, writer, maxStanzas, rs);
				}
			} else if (maxstanzas != null) {
				PreparedStatement st = dataRepository.getPreparedStatement(senderJID.getBareJID(),
						GET_MESSAGES_MAXSTANZAS_QUERY_KEY);
				synchronized (st) {
					st.setString(1, roomJID);
					maxStanzas = maxstanzas;
					rs = st.executeQuery();
					processResultSet(room, senderJID, writer, maxStanzas, rs);
				}
			} else if (seconds != null) {
				PreparedStatement st = dataRepository.getPreparedStatement(senderJID.getBareJID(),
						GET_MESSAGES_SINCE_QUERY_KEY);
				synchronized (st) {
					st.setString(1, roomJID);
					st.setLong(2, new Date().getTime() - seconds * 1000);
					rs = st.executeQuery();
					processResultSet(room, senderJID, writer, maxStanzas, rs);
				}
			} else {
				PreparedStatement st = dataRepository.getPreparedStatement(senderJID.getBareJID(),
						GET_MESSAGES_MAXSTANZAS_QUERY_KEY);
				synchronized (st) {
					st.setString(1, roomJID);
					maxStanzas = 20;
					rs = st.executeQuery();
					processResultSet(room, senderJID, writer, maxStanzas, rs);
				}
			}

		} catch (Exception e) {
			if (log.isLoggable(Level.SEVERE))
				log.log(Level.SEVERE, "Can't get history", e);
			throw new RuntimeException(e);
		} finally {
			dataRepository.release(null, rs);
		}
	}

	public void init(DataRepository dataRepository) {
		try {
			dataRepository.checkTable("muc_history", CREATE_MUC_HISTORY_TABLE_VAL);

			internalInit(dataRepository);
		} catch (SQLException e) {
			if (log.isLoggable(Level.WARNING))
				log.log(Level.WARNING, "Initializing problem", e);
			try {
				if (log.isLoggable(Level.INFO))
					log.info("Trying to create tables: " + CREATE_MUC_HISTORY_TABLE_VAL);
				Statement st = dataRepository.createStatement(null);
				st.execute(CREATE_MUC_HISTORY_TABLE_VAL);

				internalInit(dataRepository);
			} catch (SQLException e1) {
				if (log.isLoggable(Level.WARNING))
					log.log(Level.WARNING, "Can't initialize muc history", e1);
				throw new RuntimeException(e1);
			}
		}
	}

	public void setDataSource(DataRepository dataRepository) {
		init(dataRepository);
		super.setDataSource(dataRepository);
	}

	private void internalInit(DataRepository dataRepository) throws SQLException {
		dataRepository.initPreparedStatement(ADD_MESSAGE_QUERY_KEY, ADD_MESSAGE_QUERY_VAL);
		dataRepository.initPreparedStatement(DELETE_MESSAGES_QUERY_KEY, DELETE_MESSAGES_QUERY_VAL);
		dataRepository.initPreparedStatement(GET_MESSAGES_SINCE_QUERY_KEY, GET_MESSAGES_SINCE_QUERY_VAL);
		dataRepository.initPreparedStatement(GET_MESSAGES_MAXSTANZAS_QUERY_KEY, GET_MESSAGES_MAXSTANZAS_QUERY_VAL);
	}

	protected void processResultSet(Room room, JID senderJID, PacketWriter writer, Integer maxStanzas, ResultSet rs)
			throws SQLException, TigaseStringprepException {
		int i = 0;

		Affiliation recipientAffiliation = room.getAffiliation(senderJID.getBareJID());
		boolean addRealJids = room.getConfig().getRoomAnonymity() == Anonymity.nonanonymous
				|| room.getConfig().getRoomAnonymity() == Anonymity.semianonymous
						&& (recipientAffiliation == Affiliation.owner || recipientAffiliation == Affiliation.admin);

		ArrayList<Packet> result = new ArrayList<Packet>();
		for (; rs.next() && (maxStanzas == null || maxStanzas > i); i++) {
			String msgSenderNickname = rs.getString("sender_nickname");
			Date msgTimestamp = new Date(rs.getLong("timestamp"));
			String msgSenderJid = rs.getString("sender_jid");
			String body = rs.getString("body");
			String msg = rs.getString("msg");

			Packet m = createMessage(room.getRoomJID(), senderJID, msgSenderNickname, msg, body, msgSenderJid, addRealJids,
					msgTimestamp);
			result.add(0, m);
		}

		for (Packet element : result) {
			writer.write(element);
		}

	}

}
