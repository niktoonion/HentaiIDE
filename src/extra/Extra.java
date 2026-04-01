/*
 * Copyright (C) 2026 Федотов Владислав Игоревич (niktoonion)
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: GPL-3.0-or-later   // ← для машинного анализа (необязательно)
 */


package extra;



import java.awt.*;
import javax.swing.*;

public class Extra {
	
	private static Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	
	public static int sc_getWidth() {
		return (int) screenSize.getWidth();
	}
	
	public static int sc_getHeight() {
		return (int) screenSize.getHeight();
	}

	
}
