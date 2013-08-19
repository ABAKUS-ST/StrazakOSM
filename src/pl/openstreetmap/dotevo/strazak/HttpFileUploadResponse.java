package pl.openstreetmap.dotevo.strazak;

import java.io.File;

public interface HttpFileUploadResponse {
	public void uploadResponse(int code, File file);
}
