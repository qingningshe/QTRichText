# QTRichText
该控件是在[RichText](https://github.com/zzhoujay/RichText)的基础上扩展而成，素材均来自[RichText](https://github.com/zzhoujay/RichText)，感谢zzhoujay。
## 主要特性
* 链式操作
* 支持图片点击回调
* 支持超链接点击回调
* 支持图片三种对齐方式（左对齐、居中、右对齐）
* 支持单张图片的url、大小、对齐方式的精细控制
* 不依赖任何图片加载三方库，无侵入式。支持任何图片加载库。
## 使用
        richText
                .imgClick(new QTRichText.OnImgClickListener() {
                    @Override
                    public void onClick(ArrayList<String> imgUrls, int position) {
                        Log.i(TAG, "图片-->" + imgUrls.get(position));
                    }
                })
                .linkClick(new QTRichText.OnLinkClickListener() {
                    @Override
                    public boolean onClick(String url) {
                        Log.i(TAG, "链接-->" + url);
                        return true;
                    }
                })
                .imgLoad(new QTRichText.OnImgLoadListener() {
                    @Override
                    public void onFix(QTRichText.ImageHolder holder) {
                        if (holder.getPosition() % 3 == 0) {
                            holder.setStyle(QTRichText.Style.LEFT);
                        } else if (holder.getPosition() % 3 == 1) {
                            holder.setStyle(QTRichText.Style.CENTER);
                        } else {
                            holder.setStyle(QTRichText.Style.RIGHT);
                        }
                        
                        //设置宽高
                        holder.setWidth(550);
                        holder.setHeight(400);
                    }

                    @Override
                    public void loadBmp(final QTRichText richText, final QTRichText.UrlDrawable drawable, final QTRichText.ImageHolder holder) {
                        //下载图片
                        UILKit.getLoader().loadImage(holder.getSrc(), UILKit.getPicOptions(), new SimpleImageLoadingListener() {

                            @Override
                            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                                super.onLoadingComplete(imageUri, view, loadedImage);
                                richText.fillBmp(drawable, holder, loadedImage);        //必须调用
                            }
                        });
                    }
                })
                .DEBUG(false)
                .text(TEXT);

#gradle使用
` compile 'com.qingningshe.richtext:richtext:1.0.0'`
## 运行截图
![](https://github.com/qingningshe/QTRichText/blob/master/screenshot.jpeg)


