# RS 3D API security baseline

The generator is designed to run privately on the owner's machine/server.

- Bind to 127.0.0.1 during initial tower testing.
- Set RS3D_API_KEY before exposing the service beyond localhost.
- Never expose model directories as static files.
- Uploaded files are stored inside RS3D_WORK_DIR using server-created UUID job folders.
- Filenames supplied by clients are not used as paths.
- Input type and maximum upload size are enforced.
- Backend commands use argument arrays rather than shell interpolation.
- Jobs expose only validated IDs and the server resolves paths under RS3D_WORK_DIR.
- Do not port-forward 8787 to the public internet until TLS/reverse-proxy authentication is configured.
