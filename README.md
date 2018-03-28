# Jugg   模拟Picasso图片加载框架

a

**An Android image downloading and caching library writen by *Ryan Ju


***
------------------------------------------------------------------------
----------
it's easy to use in your ImageView like : 


    Jugg.withContext(context).withUrl(httpUrl).show(imageView);





----------


![][1]


you can also set place holder ,error holder like:


    Jugg.withContext(context).withUrl(httpUrl).errorholder(R.drawable.error).placeholder(R.drawable.loading).show(imageView);





if needed,set retry times when failed to load images

you can get loading callback by implements the LoadCallback interface and add to the method `show(imageView,callback).



`

**And...functions like resize,rotate,centerCrop,centerInside,fitXY are supported!**


  [1]: http://h.picphotos.baidu.com/album/s=550;q=90;c=xiangce,100,100/sign=2725ae78209759ee4e5060ce82c0322b/503d269759ee3d6d7a244dbe45166d224e4adee1.jpg?referer=3a11c996d339b60014d93b871abf&x=.jpg
