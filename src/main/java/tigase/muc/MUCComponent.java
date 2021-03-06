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
package tigase.muc;

import tigase.component.AbstractKernelBasedComponent;
import tigase.component.exceptions.RepositoryException;
import tigase.component.modules.impl.AdHocCommandModule;
import tigase.component.modules.impl.JabberVersionModule;
import tigase.component.modules.impl.XmppPingModule;
import tigase.form.Field;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.BeanSelector;
import tigase.kernel.beans.Inject;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.core.Kernel;
import tigase.muc.modules.*;
import tigase.muc.repository.IMucRepository;
import tigase.server.Packet;
import tigase.xmpp.mam.MAMItemHandler;
import tigase.xmpp.mam.MAMQueryParser;
import tigase.xmpp.mam.modules.GetFormModule;

import javax.script.Bindings;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

@Bean(name = "muc", parent = Kernel.class, active = false, selectors = {BeanSelector.NonClusterMode.class})
public class MUCComponent extends AbstractKernelBasedComponent {

	public static final String DEFAULT_ROOM_CONFIG_KEY = "default_room_config";
	public static final String DEFAULT_ROOM_CONFIG_PREFIX_KEY = DEFAULT_ROOM_CONFIG_KEY + "/";
	@Inject
	private Ghostbuster2 ghostbuster;

	@ConfigField(alias = DEFAULT_ROOM_CONFIG_KEY, desc = "Default room configuration", allowAliasFromParent = false)
	private Map<String, String> defaultRoomConfig = new HashMap<>();

	public MUCComponent() {
	}

	protected static void addIfExists(Bindings binds, String name, Object value) {
		if (value != null) {
			binds.put(name, value);
		}
	}

	@Override
	public String getComponentVersion() {
		String version = this.getClass().getPackage().getImplementationVersion();
		return version == null ? "0.0.0" : version;
	}

	@Override
	public String getDiscoCategory() {
		return "conference";
	}

	@Override
	public String getDiscoCategoryType() {
		return "text";
	}

	@Override
	public String getDiscoDescription() {
		return "Multi User Chat";
	}

	@Override
	public int hashCodeForPacket(Packet packet) {
		if ((packet.getStanzaFrom() != null) && (packet.getPacketFrom() != null)
				&& !getComponentId().equals(packet.getPacketFrom())) {
			return packet.getStanzaFrom().hashCode();
		}

		if (packet.getStanzaTo() != null) {
			return packet.getStanzaTo().hashCode();
		}

		return 1;
	}

	@Override
	public boolean isDiscoNonAdmin() {
		return true;
	}

	@Override
	public boolean isSubdomain() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void processPacket(Packet packet) {
		if (ghostbuster != null) {
			try {
				ghostbuster.update(packet);
			} catch (Exception e) {
				log.log(Level.WARNING, "There is no Dana, there is only Zuul", e);
			}
		}
		super.processPacket(packet);
	}

	@Override
	public int processingInThreads() {
		return Runtime.getRuntime().availableProcessors() * 4;
	}

	@Override
	public int processingOutThreads() {
		return Runtime.getRuntime().availableProcessors() * 4;
	}

	@Override
	protected void registerModules(Kernel kernel) {
		kernel.registerBean(XmppPingModule.class).exec();
		kernel.registerBean(JabberVersionModule.class).exec();
		//kernel.registerBean(DiscoveryModule.class).exec();
		kernel.registerBean(GroupchatMessageModule.class).exec();
		kernel.registerBean(IqStanzaForwarderModule.class).exec();
		kernel.registerBean(MediatedInvitationModule.class).exec();
		kernel.registerBean(ModeratorModule.class).exec();
		//kernel.registerBean(PresenceModuleImpl.class).exec();
		kernel.registerBean(PrivateMessageModule.class).exec();
		kernel.registerBean(RoomConfigurationModule.class).exec();
		kernel.registerBean(UniqueRoomNameModule.class).exec();
		kernel.registerBean(AdHocCommandModule.class).exec();

		kernel.registerBean(MAMItemHandler.class).exec();
		kernel.registerBean(MAMQueryParser.class).exec();
		kernel.registerBean(MAMQueryModule.class).exec();
		kernel.registerBean(GetFormModule.class).exec();
		//kernel.registerBean(MUCConfig.class).exec();

//		kernel.registerBean(IMucRepository.ID).asClass(InMemoryMucRepository.class).exec();
		//kernel.registerBean(RoomChatLogger.class).exec();
		//kernel.registerBean(Ghostbuster2.class).exec();
	}

	@Override
	public void initialize() {
		try {
			updateDefaultRoomConfig();
		} catch (Exception ex) {
			log.log(Level.FINEST, "Exception during modification of default room config", ex);
		}

		super.initialize();
	}

	private void updateDefaultRoomConfig() throws RepositoryException {
		final IMucRepository mucRepository = kernel.getInstance(IMucRepository.class);

		if (defaultRoomConfig.isEmpty())
			return;

		log.config("Updating Default Room Config");

		final RoomConfig defaultRoomConfig = mucRepository.getDefaultRoomConfig();
		boolean changed = false;
		for (Entry<String, String> x : this.defaultRoomConfig.entrySet()) {
			String var = x.getKey();
			Field field = defaultRoomConfig.getConfigForm().get(var);
			if (field != null) {
				changed = true;
				String[] values = x.getValue().split(",");
				field.setValues(values);
			} else if (log.isLoggable(Level.WARNING)) {
				log.warning("Default config room doesn't contains variable '" + var + "'!");
			}
		}
		if (changed) {
			if (log.isLoggable(Level.CONFIG))
				log.config("Default room configuration is udpated");
			mucRepository.updateDefaultRoomConfig(defaultRoomConfig);
		}

	}

}
