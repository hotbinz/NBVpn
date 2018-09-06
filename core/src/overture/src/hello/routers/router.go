package routers

import (
	"hello/controllers"
	"github.com/astaxie/beego"
	"github.com/beego/admin"
)

func init() {
	admin.Run()
	beego.Router("/", &controllers.MainController{})
	beego.Router("/api/video/", &controllers.ApiController{},"get:ListVideo;post:CreateVideo;put:UpdateVideo;delete:DeleteVideo")
	beego.Router("/api/video/:url", &controllers.ApiController{},"get:GetVideoPlyer")
}
