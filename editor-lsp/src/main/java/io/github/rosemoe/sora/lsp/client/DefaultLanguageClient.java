/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.sora.lsp.client;

import android.util.Log;

import org.eclipse.lsp4j.ApplyWorkspaceEditParams;
import org.eclipse.lsp4j.ApplyWorkspaceEditResponse;
import org.eclipse.lsp4j.ConfigurationParams;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.UnregistrationParams;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.services.LanguageClient;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DefaultLanguageClient implements LanguageClient {

    private static String TAG = "DefaultLanguageClient";

    private final ClientContext context;


    public DefaultLanguageClient(@NotNull ClientContext context) {
        this.context = context;
    }

    @Override
    public CompletableFuture<ApplyWorkspaceEditResponse> applyEdit(ApplyWorkspaceEditParams params) {
       /* boolean response = WorkspaceEditHandler.applyEdit(params.getEdit(), "LSP edits");*/
        //TODO: implement applyEdit
        return CompletableFuture.supplyAsync(() -> new ApplyWorkspaceEditResponse(false));
    }

    @Override
    public CompletableFuture<List<Object>> configuration(ConfigurationParams configurationParams) {
        return LanguageClient.super.configuration(configurationParams);
    }

    @Override
    public CompletableFuture<List<WorkspaceFolder>> workspaceFolders() {
        return LanguageClient.super.workspaceFolders();
    }

    @Override
    public CompletableFuture<Void> registerCapability(RegistrationParams params) {
        /*return CompletableFuture.runAsync(() -> params.getRegistrations().forEach(r -> {
            String id = r.getId();
            Optional<DynamicRegistrationMethods> method = DynamicRegistrationMethods.forName(r.getMethod());
            method.ifPresent(dynamicRegistrationMethods -> registrations.put(id, dynamicRegistrationMethods));

        }));*/
        //TODO: implement registerCapability
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> unregisterCapability(UnregistrationParams params) {
       /* return CompletableFuture.runAsync(() -> params.getUnregisterations().forEach((Unregistration r) -> {
            String id = r.getId();
            Optional<DynamicRegistrationMethods> method = DynamicRegistrationMethods.forName(r.getMethod());
            if (registrations.containsKey(id)) {
                registrations.remove(id);
            } else {
                Map<DynamicRegistrationMethods, String> inverted = new HashMap<>();
                for (Map.Entry<String, DynamicRegistrationMethods> entry : registrations.entrySet()) {
                    inverted.put(entry.getValue(), entry.getKey());
                }
                if (method.isPresent() && inverted.containsKey(method.get())) {
                    registrations.remove(inverted.get(method.get()));
                }
            }
        }));*/
        //TODO: implement unregisterCapability
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void telemetryEvent(Object o) {
        Log.i(TAG, "telemetryEvent: "+o.toString());
    }

    @Override
    public void publishDiagnostics(PublishDiagnosticsParams publishDiagnosticsParams) {
      /*  String uri = FileUtils.sanitizeURI(publishDiagnosticsParams.getUri());
        List<Diagnostic> diagnostics = publishDiagnosticsParams.getDiagnostics();
        Set<EditorEventManager> managers = EditorEventManagerBase.managersForUri(uri);
        for (EditorEventManager manager: managers) {
            manager.diagnostics(diagnostics);
        }*/

        //TODO: implement publishDiagnostics
    }

    @Override
    public void showMessage(MessageParams messageParams) {
        context.getEventListener().onShowMessage(messageParams);
    }

    @Override
    public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams showMessageRequestParams) {
        //TODO: implement showMessageRequest
        return CompletableFuture.completedFuture(null);
    }


    @Override
    public void logMessage(MessageParams messageParams) {
        context.getEventListener().onLogMessage(messageParams);
    }

    @NotNull
    protected final ClientContext getContext() {
        return context;
    }
}