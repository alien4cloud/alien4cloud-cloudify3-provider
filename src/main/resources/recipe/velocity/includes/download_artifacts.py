def download(child_rel_path, child_abs_path, download_dir):
    artifact_downloaded_path = ctx.download_resource(child_abs_path)
    new_file = os.path.join(download_dir, child_rel_path)
    new_file_dir = os.path.dirname(new_file)
    if not os.path.exists(new_file_dir):
        os.makedirs(new_file_dir)
    os.rename(artifact_downloaded_path, new_file)
    ctx.logger.debug('Downloaded artifact from path ' + child_abs_path + ', it\'s available now at ' + new_file)
    return new_file


def download_artifacts(artifacts, download_dir):
    downloaded_artifacts = {}
    if not os.path.exists(download_dir):
        os.makedirs(download_dir)
    for artifact_name, artifact_ref in artifacts.items():
        ctx.logger.debug('Download artifact ' + artifact_name)
        if isinstance(artifact_ref, basestring):
            downloaded_artifacts[artifact_name] = download(os.path.basename(artifact_ref), artifact_ref, download_dir)
        else:
            child_download_dir = os.path.join(download_dir, artifact_name)
            for child_path in artifact_ref:
                download(child_path['relative_path'], child_path['absolute_path'], child_download_dir)
            downloaded_artifacts[artifact_name] = child_download_dir
    return downloaded_artifacts
