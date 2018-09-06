package controllers

import (
	"github.com/astaxie/beego"
	"hello/models"
	"encoding/base64"
)

type ApiController struct {
	beego.Controller
}

func (c *ApiController) ListVideo() {
	data := models.GetVideoList()
	c.Data["json"] = map[string]interface{}{"code":0, "msg": "success", "data": data}
	c.ServeJSON()
}
func (c *ApiController) CreateVideo() {

}
func (c *ApiController) UpdateVideo() {

}
func (c *ApiController) DeleteVideo() {

}
func (c *ApiController) GetVideoPlyer() {
	decoded, err := base64.StdEncoding.DecodeString(c.Ctx.Input.Param(":url"))
	beego.Info("GetVideoPlyer:" + string(decoded))
	if(err != nil) {
		c.Ctx.WriteString("error DecodeString")
		return
	} else {
		body, err := models.GetVideoPlyer(string(decoded))
		if(nil != err) {
			beego.Error(err.Error())
			c.Ctx.WriteString("error GetRemoteVideoPlyer")
			return
		} else {
			c.Ctx.Output.Body(body)
		}		
	}
}