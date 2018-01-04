package org.readium.r2.testapp.permissions

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.View
import io.reactivex.subjects.PublishSubject
import org.readium.r2.testapp.R

/**
 * Simple helper for obtaining android permissions
 */
class PermissionHelper(private val activity: Activity, private val permissions: Permissions) {

  private val PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE
  private val permissionDialogConfirmed = PublishSubject.create<Unit>()

  fun storagePermission(gotPermission: () -> Unit = {}) {
    val root = activity.findViewById<View>(android.R.id.content)
    permissions.request(PERMISSION)
        .toObservable()
        .repeatWhen { it.flatMap { permissionDialogConfirmed } }
        .subscribe {
          when (it!!) {
            Permissions.PermissionResult.GRANTED -> gotPermission()
            Permissions.PermissionResult.DENIED_FOREVER -> handleDeniedForever(root)
            Permissions.PermissionResult.DENIED_ASK_AGAIN -> showRationale(root) {
              permissionDialogConfirmed.onNext(Unit)
            }
          }
        }
  }

  private fun showRationale(root: View, listener: () -> Unit) {
    PermissionSnackbar.make(
            root = root,
            text = root.context.getString(R.string.permission_external_new_explanation),
            action = root.context.getString(R.string.permission_retry),
            listener = listener
    )
  }

  private fun handleDeniedForever(root: View) {
    val context = root.context
    PermissionSnackbar.make(
            root = root,
            text = context.getString(R.string.permission_external_new_explanation),
            action = context.getString(R.string.permission_goto_settings)
    ) {
      val intent = Intent()
      intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
      val uri = Uri.fromParts("package", context.packageName, null)
      intent.data = uri
      context.startActivity(intent)
    }
  }
}
