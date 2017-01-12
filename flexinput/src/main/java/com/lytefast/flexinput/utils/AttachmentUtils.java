package com.lytefast.flexinput.utils;

import android.net.Uri;

import com.lytefast.flexinput.model.Attachment;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * @author Sam Shih
 */
public class AttachmentUtils {

  public static List<Attachment> transform(Collection<File> files) {
    ArrayList<Attachment> attachments = new ArrayList<>(files.size());
    for (File f : files) {
      attachments.add(new Attachment(f.hashCode(), Uri.fromFile(f), f.getName()));
    }
    return attachments;
  }
}
