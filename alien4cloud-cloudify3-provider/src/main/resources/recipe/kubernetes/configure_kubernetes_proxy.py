from cloudify import ctx

def configure_master_ip(master_ip):
  ctx.logger.info("Using Kubernetes Master at {}".format(master_ip))
  kubernetes_info = { 'url' : master_ip }
  ctx.instance.runtime_properties['kubernetes_info'] = kubernetes_info