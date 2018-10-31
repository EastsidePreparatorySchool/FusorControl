function HttpClient() {
  this.convert = (params) => {
    let fd = new FormData();
    for (let i = 0; i < Object.keys(params).length; i++) {
      key = Object.keys(params)[i]
      fd.append(key, String(params[key]));
    }
    return fd;
  };
  this.xhr = (type, url, params) => {
    return new Promise((resolve, reject) => {
      let httpRequest = new XMLHttpRequest();
      httpRequest.onreadystatechange = () => {
        if (httpRequest.status == 310) {
          close();
          window.location.replace(httpRequest.getResponseHeader('Location'));
        } else if (httpRequest.readyState == 4) resolve({
          status: httpRequest.status,
          body: (httpRequest.response) ? JSON.parse(httpRequest.response) : null
         });
      }
      httpRequest.open(type, url, true);
      httpRequest.send(params);
    });
  };
  this.get = (url) => {return this.xhr('GET', url, null)};
  this.post = (url, params) => {return this.xhr('POST', url, this.convert(params))};
  this.put = (url, params) => {return this.xhr('PUT', url, this.convert(params))};
  this.delete = (url, params) => {return this.xhr('DELETE', url, this.convert(params))};
}
const hc = new HttpClient();
