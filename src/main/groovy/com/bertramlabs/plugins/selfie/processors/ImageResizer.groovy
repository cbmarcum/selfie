package com.bertramlabs.plugins.selfie.processors

import com.bertramlabs.plugins.selfie.Attachment
import javax.imageio.ImageIO
import org.imgscalr.Scalr
import groovy.util.logging.Commons

@Commons
class ImageResizer {

	private static final Map<String, String> CONTENT_TYPE_TO_NAME =
		['image/jpeg': 'jpeg', 'image/png': 'png', 'image/bmp': 'bmp', 'image/gif': 'gif']

	Attachment attachment

	def process() {
		def formatName = formatNameFromContentType(attachment.contentType)
		if (!formatName) {
			return
		}
		def styleOptions = attachment.options?.styles
		def image = ImageIO.read(attachment.inputStream)
		if(styleOptions) {
			for (style in styleOptions) {
				processStyle(style.key, [format: formatName] + style.value.clone(),image)
			}	
		}
		
	}

	def process(typeName,styleOptions) {
		def formatName = formatNameFromContentType(attachment.contentType)
		if (!formatName) {
			return
		}
		def image = ImageIO.read(attachment.inputStream)
		processStyle(typeName, [format: formatName] + styleOptions.clone(), image)
	}

	def processStyle(typeName, options, image) {
		// try {
			def typeFileName = attachment.fileNameForType(typeName)
			def outputImage

			if(options.mode == 'fit' || options.mode == 'fity' || options.mode == 'fitx' || options.mode == 'fill') {
				def mode = Scalr.Mode.FIT_TO_HEIGHT
				if(options.mode == 'fit' || options.mode == 'fill') {
					if(image.width > options.width || image.height > options.height) {
						if(image.width - options.width >= image.height - options.height) {
							mode = Scalr.Mode.FIT_TO_WIDTH
						} else {
							mode = Scalr.Mode.FIT_TO_HEIGHT
						}
					} else if(image.width == options.width) {
						mode = Scalr.Mode.FIT_TO_WIDTH
					} else if (image.height == options.height) {
						mode = Scalr.Mode.FIT_TO_HEIGHT	
					} else {
						if((options.height - image.height) < (options.width - image.width)) {
							mode = Scalr.Mode.FIT_TO_HEIGHT
						} else {
							mode = Scalr.Mode.FIT_TO_WIDTH
						}
					}
					if(options.mode == 'fill'){
						mode = (mode == Scalr.Mode.FIT_TO_HEIGHT)?
							Scalr.Mode.FIT_TO_WIDTH:Scalr.Mode.FIT_TO_HEIGHT
					}
				}
				else if(options.mode == 'fitx') {
					mode = Scalr.Mode.FIT_TO_WIDTH
				} else {
					mode = Scalr.Mode.FIT_TO_HEIGHT
				}
				outputImage = Scalr.resize(image, Scalr.Method.ULTRA_QUALITY, mode, options.width, options.height, Scalr.OP_ANTIALIAS)
				def xOffset = 0
				def yOffset = 0
				if(options.x == null) {
					xOffset = Math.floor((outputImage.width - options.width) / 2).toInteger()
				}
				if(options.y == null) {
						yOffset = Math.floor((outputImage.height - options.height) / 2).toInteger()
				}
				if(xOffset >= 0 && yOffset >= 0) {
					outputImage = Scalr.crop(outputImage, xOffset,yOffset, options.width, options.height, Scalr.OP_ANTIALIAS)
				}
			} else if (options.mode == 'crop') {
				outputImage = Scalr.crop(image,options.x ?: 0,options.y ?: 0, options.width, options.height, Scalr.OP_ANTIALIAS)
			} else if (options.mode == 'scale') {
				outputImage = Scalr.resize(image, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.AUTOMATIC, options.width, options.height, Scalr.OP_ANTIALIAS)
			}

			def saveStream = new ByteArrayOutputStream()

			ImageIO.write(outputImage, options.format,saveStream)
			attachment.saveProcessedStyle(typeName,saveStream.toByteArray())
		// } catch(e) {
		// 	log.error("Error Processing Uploaded File ${attachment.fileName} - ${typeName}",e)
		// }

	}

	def formatNameFromContentType(contentType) {
		CONTENT_TYPE_TO_NAME[contentType]
	}
}
