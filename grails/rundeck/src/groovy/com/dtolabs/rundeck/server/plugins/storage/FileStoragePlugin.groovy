package com.dtolabs.rundeck.server.plugins.storage

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.storage.StoragePlugin
import org.rundeck.storage.api.Tree
import org.rundeck.storage.data.file.FileTreeUtil
import org.rundeck.storage.impl.DelegateTree

/**
 * FileStoragePlugin provides the basic file-system storage layer.
 * @author greg
 * @since 2014-02-19
 */
@Plugin(name = FileStoragePlugin.PROVIDER_NAME, service = ServiceNameConstants.Storage)
class FileStoragePlugin extends DelegateTree<ResourceMeta> implements StoragePlugin {
    public static final String PROVIDER_NAME = "file"
    @PluginProperty(description = "Base directory", required = true)
    String baseDir

    Tree<ResourceMeta> delegateTree

    public Tree<ResourceMeta> getDelegate() {
        if (null == delegateTree) {
            if(null==baseDir) {
                throw new IllegalArgumentException("baseDir is not set")
            }
            def file = new File(baseDir)
            delegateTree = FileTreeUtil.forRoot(file,StorageUtil.factory())
        }
        return delegateTree;
    }
}
