/*
 * MucDAOMDBean.java
 *
 * Tigase Multi User Chat Component
 * Copyright (C) 2004-2016 "Tigase, Inc." <office@tigase.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.muc.repository;

import tigase.component.exceptions.RepositoryException;
import tigase.db.DBInitException;
import tigase.db.DataSource;
import tigase.db.DataSourceHelper;
import tigase.db.beans.MDRepositoryBean;
import tigase.db.beans.MDRepositoryBeanWithStatistics;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.muc.Affiliation;
import tigase.muc.MUCComponent;
import tigase.muc.RoomConfig;
import tigase.muc.RoomWithId;
import tigase.server.BasicComponent;
import tigase.xmpp.BareJID;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by andrzej on 16.10.2016.
 */
@Bean(name = "muc-dao", parent = MUCComponent.class)
public class MucDAOMDBean
		extends MDRepositoryBeanWithStatistics<IMucDAO>
		implements IMucDAO {

	private static final Logger log = Logger.getLogger(MucDAOMDBean.class.getCanonicalName());

	@ConfigField(desc = "Use domain without component name to lookup for repository", alias = "map-component-to-bare-domain")
	private boolean mapComponentToBareDomain = false;

	public MucDAOMDBean() {
		super(IMucDAO.class);
	}

	@Override
	public boolean belongsTo(Class<? extends BasicComponent> component) {
		return MUCComponent.class.isAssignableFrom(component);
	}

	@Override
	public Object createRoom(RoomWithId room) throws RepositoryException {
		return getRepository(room.getRoomJID().getDomain()).createRoom(room);
	}

	@Override
	public void destroyRoom(BareJID roomJID) throws RepositoryException {
		getRepository(roomJID.getDomain()).destroyRoom(roomJID);
	}

	@Override
	public Map<BareJID, Affiliation> getAffiliations(RoomWithId room) throws RepositoryException {
		return getRepository(room.getRoomJID().getDomain()).getAffiliations(room);
	}

	@Override
	public RoomWithId getRoom(BareJID roomJID) throws RepositoryException {
		return getRepository(roomJID.getDomain()).getRoom(roomJID);
	}

	@Override
	public List<BareJID> getRoomsJIDList() throws RepositoryException {
		return repositoriesStream().flatMap(repo -> {
			Stream<BareJID> result = null;
			try {
				result = repo.getRoomsJIDList().stream();
			} catch (RepositoryException e) {
				log.log(Level.WARNING, "Failed to retrieve list of room jids from " + repo.toString(), e);
				result = Stream.empty();
			}
			return result;
		}).collect(Collectors.toList());
	}

	@Override
	public void setAffiliation(RoomWithId room, BareJID jid, Affiliation affiliation) throws RepositoryException {
		getRepository(room.getRoomJID().getDomain()).setAffiliation(room, jid, affiliation);
	}

	@Override
	public void setSubject(RoomWithId room, String subject, String creatorNickname, Date changeDate)
			throws RepositoryException {
		getRepository(room.getRoomJID().getDomain()).setSubject(room, subject, creatorNickname, changeDate);
	}

	@Override
	public void updateRoomConfig(RoomConfig roomConfig) throws RepositoryException {
		getRepository(roomConfig.getRoomJID().getDomain()).updateRoomConfig(roomConfig);
	}

	@Override
	public Class<?> getDefaultBeanClass() {
		return MucDAOProviderConfigBean.class;
	}

	@Override
	protected IMucDAO getRepository(String domain) {
		if (mapComponentToBareDomain) {
			int idx = domain.indexOf(".");
			if (idx > 0) {
				domain = domain.substring(idx + 1);
			}
		}
		return super.getRepository(domain);
	}

	@Override
	public void setDataSource(DataSource dataSource) {
	}

	@Override
	protected Class<? extends IMucDAO> findClassForDataSource(DataSource dataSource) throws DBInitException {
		return DataSourceHelper.getDefaultClass(IMucDAO.class, dataSource.getResourceUri());
	}

	public static class MucDAOProviderConfigBean extends MDRepositoryBean.MDRepositoryConfigBean<IMucDAO> {

	}

}
