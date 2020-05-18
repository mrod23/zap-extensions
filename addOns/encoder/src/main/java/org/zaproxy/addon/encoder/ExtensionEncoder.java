/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2018 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.zaproxy.addon.encoder;

import java.awt.Frame;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.text.JTextComponent;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;
import org.parosproxy.paros.Constant;
import org.parosproxy.paros.control.Control;
import org.parosproxy.paros.extension.Extension;
import org.parosproxy.paros.extension.ExtensionAdaptor;
import org.parosproxy.paros.extension.ExtensionHook;
import org.parosproxy.paros.view.View;
import org.zaproxy.zap.extension.script.ExtensionScript;
import org.zaproxy.zap.extension.script.ScriptType;
import org.zaproxy.zap.extension.script.ScriptWrapper;
import org.zaproxy.zap.view.ZapMenuItem;

public class ExtensionEncoder extends ExtensionAdaptor {

    public static final String SCRIPT_TYPE_ENCODE_DECODE = "encode-decode";
    public static final String NAME = "ExtensionEncoder";
    public static final int EXTENSION_ORDER = 87;
    public static final ImageIcon ICON;
    private static final Logger LOGGER = Logger.getLogger(ExtensionEncoder.class);
    private static final List<Class<? extends Extension>> EXTENSION_DEPENDENCIES;

    static {
        List<Class<? extends Extension>> dependencies = new ArrayList<>(1);
        dependencies.add(ExtensionScript.class);
        EXTENSION_DEPENDENCIES = Collections.unmodifiableList(dependencies);
        ICON = createIcon("encoder.png");
    }

    private ScriptType encodeScriptType = null;
    private EncodeDecodeDialog encodeDecodeDialog = null;
    private PopupEncoderMenu popupEncodeMenu = null;
    private ZapMenuItem toolsMenuEncoder = null;
    private PopupEncoderDeleteOutputPanelMenu popupDeleteOutputMenu;

    public ExtensionEncoder() {
        super(NAME);
        this.setOrder(EXTENSION_ORDER);
    }

    private static ImageIcon createIcon(String iconName) {
        if (View.isInitialised()) {
            return new ImageIcon(
                    ExtensionEncoder.class.getResource(
                            "/org/zaproxy/addon/encoder/resources/" + iconName));
        }
        return null;
    }

    public static ExtensionScript getExtensionScript() {
        return Control.getSingleton().getExtensionLoader().getExtension(ExtensionScript.class);
    }

    public static List<ScriptWrapper> getEncodeDecodeScripts() {
        ExtensionScript extensionScript = getExtensionScript();

        return extensionScript.getScripts(ExtensionEncoder.SCRIPT_TYPE_ENCODE_DECODE);
    }

    private ScriptType getEncoderScriptType() {
        if (encodeScriptType == null) {
            encodeScriptType =
                    new ScriptType(
                            SCRIPT_TYPE_ENCODE_DECODE,
                            "encoder.scripts.type.encodedecode",
                            createIcon("script-encoder.png"),
                            true);
        }
        return encodeScriptType;
    }

    @Override
    public void hook(ExtensionHook extensionHook) {
        super.hook(extensionHook);

        if (getView() != null) {
            extensionHook.getHookMenu().addPopupMenuItem(getPopupMenuEncode());
            extensionHook.getHookMenu().addPopupMenuItem(getPopupMenuDeleteOutputPanel());
            extensionHook.getHookMenu().addToolsMenuItem(getToolsMenuItemEncoder());
        }

        ExtensionScript extScript = getExtensionScript();
        extScript.registerScriptType(getEncoderScriptType());
    }

    private ZapMenuItem getToolsMenuItemEncoder() {
        if (toolsMenuEncoder == null) {
            toolsMenuEncoder = new ZapMenuItem("encoder.tools.menu.encdec");
            toolsMenuEncoder.addActionListener(e -> showEncodeDecodeDialog(null));
        }
        return toolsMenuEncoder;
    }

    private PopupEncoderMenu getPopupMenuEncode() {
        if (popupEncodeMenu == null) {
            popupEncodeMenu = new PopupEncoderMenu();
            popupEncodeMenu.setText(Constant.messages.getString("encoder.popup.title"));
            popupEncodeMenu.addActionListener(
                    e -> showEncodeDecodeDialog(popupEncodeMenu.getLastInvoker()));
        }
        return popupEncodeMenu;
    }

    private PopupEncoderDeleteOutputPanelMenu getPopupMenuDeleteOutputPanel() {
        if (popupDeleteOutputMenu == null) {
            popupDeleteOutputMenu = new PopupEncoderDeleteOutputPanelMenu();
            popupDeleteOutputMenu.setText(Constant.messages.getString("encoder.popup.delete"));
            popupDeleteOutputMenu.addActionListener(
                    e -> {
                        EncodeDecodeDialog encodeDecodeDialog = showEncodeDecodeDialog(null);
                        encodeDecodeDialog.deleteOutputPanel(
                                popupDeleteOutputMenu.getLastInvoker());
                    });
        }
        return popupDeleteOutputMenu;
    }

    private EncodeDecodeDialog showEncodeDecodeDialog(JTextComponent lastInvoker) {

        List<TabModel> tabModels = new ArrayList<>();
        try {
            tabModels = EncoderConfig.loadConfig();
        } catch (ConfigurationException | IOException e) {
            LOGGER.error("Can not load Encoder Config");
        }

        if (encodeDecodeDialog == null) {
            encodeDecodeDialog = new EncodeDecodeDialog(tabModels);
        } else {
            if ((encodeDecodeDialog.getState() & Frame.ICONIFIED) == Frame.ICONIFIED) {
                // bring up to front if iconfied
                encodeDecodeDialog.setState(Frame.NORMAL);
            }
        }

        encodeDecodeDialog.setVisible(true);

        if (lastInvoker != null) {
            encodeDecodeDialog.setInputField(lastInvoker.getSelectedText());
        }
        return encodeDecodeDialog;
    }

    @Override
    public boolean canUnload() {
        return true;
    }

    @Override
    public void unload() {
        super.unload();
        ExtensionScript extScript = getExtensionScript();
        extScript.removeScriptType(getEncoderScriptType());
    }

    @Override
    public String getAuthor() {
        return Constant.ZAP_TEAM;
    }

    @Override
    public String getUIName() {
        return Constant.messages.getString("encoder.name");
    }

    @Override
    public String getDescription() {
        return Constant.messages.getString("encoder.desc");
    }

    @Override
    public List<Class<? extends Extension>> getDependencies() {
        return EXTENSION_DEPENDENCIES;
    }
}
