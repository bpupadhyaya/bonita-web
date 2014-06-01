/**
 * Copyright (C) 2013 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2.0 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.bonitasoft.web.toolkit.client.ui.component.core;

import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by Vincent Elcrin
 * Date: 02/10/13
 * Time: 16:22
 */
public class CustomUiComponent extends UiComponent {

    public CustomUiComponent(final UIObject uiObject) {
        super(uiObject);
    }

    @Override
    protected Element makeElement() {

        if (uiObject instanceof Widget) {
            adopt((Widget) uiObject);
        }

        return super.makeElement();
    }

    class WidgetWrapper extends SimplePanel {

        WidgetWrapper(final Widget child) {
            super(child);
        }

        public void attach() {
            onAttach();
        }
    }

    private void adopt(final Widget widget) {
        final WidgetWrapper wrapper = new WidgetWrapper(widget);
        wrapper.attach();
        RootPanel.detachOnWindowClose(wrapper);
    }
}