package com.mcc.g22

class ImageModel {

    var name: String? = null
    var image_drawable: Int = 0

    // TODO Uri of imege to show
    // TODO storage reference of image to show

    fun getNames(): String {
        return name.toString()
    }

    fun setNames(name: String) {
        this.name = name
    }

    fun getImage_drawables(): Int {
        return image_drawable
    }

    fun setImage_drawables(image_drawable: Int) {
        this.image_drawable = image_drawable
    }

}