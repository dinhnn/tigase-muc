/*
 * Tigase Jabber/XMPP Multi-User Chat Component
 * Copyright (C) 2008 "Bartosz M. Małkowski" <bartosz.malkowski@tigase.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 *
 * $Rev$
 * Last modified by $Author$
 * $Date$
 */
package tigase.muc;

import tigase.component.PacketWriter;
import tigase.kernel.core.Kernel;
import tigase.muc.repository.IMucRepository;

/**
 * @author bmalkow
 *
 */
public class TestMUCCompoent extends MUCComponent {

	private IMucRepository mucRepository;
	private PacketWriter writer;

	public TestMUCCompoent(PacketWriter writer, IMucRepository mucRepository) {
		this.writer = writer;
		this.mucRepository = mucRepository;
	}

	public IMucRepository getMucRepository() {
		return this.mucRepository;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see tigase.muc.MUCComponent#registerModules(tigase.kernel.core.Kernel)
	 */
	@Override
	protected void registerModules(Kernel kernel) {
		kernel.registerBean("writer").asInstance(writer).exec();
		super.registerModules(kernel);
	}

}
